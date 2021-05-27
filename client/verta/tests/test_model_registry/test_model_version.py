# -*- coding: utf-8 -*-

import filecmp
import glob
import os
import pickle
import shutil
import sys
import tarfile
import tempfile
import zipfile

import cloudpickle
import pytest
import requests
import six

import verta
import verta.dataset
from verta import visibility
from verta.environment import Python
from verta.tracking.entities._deployable_entity import _CACHE_DIR
from verta.endpoint.update import DirectUpdateStrategy
from verta.registry import lock
from verta._internal_utils import (
    _artifact_utils,
    _utils,
)

from .. import utils


pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


class TestMDBIntegration:
    def test_from_run(self, experiment_run, model_for_deployment, registered_model):
        np = pytest.importorskip("numpy")

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        artifact = np.random.random((36, 12))
        experiment_run.log_artifact("some-artifact", artifact)

        model_version = registered_model.create_version_from_run(
            run_id=experiment_run.id,
            name="From Run {}".format(experiment_run.id),
        )

        env_str = str(model_version.get_environment())
        assert 'scikit-learn' in env_str
        assert 'Python' in env_str

        assert model_for_deployment['model'].get_params() == model_version.get_model().get_params()
        assert np.array_equal(model_version.get_artifact("some-artifact"), artifact)

    def test_from_run_diff_workspaces(self, client, experiment_run, organization, created_entities):
        registered_model = client.create_registered_model(workspace=organization.name)
        created_entities.append(registered_model)

        model_version = registered_model.create_version_from_run(
            run_id=experiment_run.id,
            name="From Run {}".format(experiment_run.id)
        )

        assert model_version.workspace != experiment_run.workspace

    def test_from_run_diff_workspaces_no_access_error(self, experiment_run, client_2, created_entities):
        registered_model = client_2.create_registered_model()
        created_entities.append(registered_model)

        with pytest.raises(requests.HTTPError) as excinfo:
            registered_model.create_version_from_run(
                run_id=experiment_run.id,
                name="From Run {}".format(experiment_run.id)
            )

        exc_msg = str(excinfo.value).strip()
        assert exc_msg.startswith("404")
        assert "not found" in exc_msg


class TestModelVersion:
    def test_create(self, registered_model):
        name = verta._internal_utils._utils.generate_default_name()
        assert registered_model.create_version(name)
        with pytest.raises(requests.HTTPError) as excinfo:
            assert registered_model.create_version(name)
        excinfo_value = str(excinfo.value).strip()
        assert "409" in excinfo_value
        assert "already exists" in excinfo_value

    def test_set(self, registered_model):
        name = verta._internal_utils._utils.generate_default_name()
        version = registered_model.set_version(name=name)

        assert registered_model.set_version(name=version.name).id == version.id

    def test_get_by_name(self, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")
        retrieved_model_version = registered_model.get_version(name=model_version.name)
        assert retrieved_model_version.id == model_version.id

    def test_get_by_id(self, registered_model):
        model_version = registered_model.get_or_create_version()
        retrieved_model_version = registered_model.get_version(id=model_version.id)
        assert model_version.id == retrieved_model_version.id
        with pytest.warns(UserWarning, match='.*already exists.*'):
            registered_model.get_or_create_version(id=model_version.id, desc="new description")

    def test_repr(self, model_version):
        model_version.add_labels(["tag1", "tag2"])
        repr = str(model_version)

        assert model_version.name in repr
        assert str(model_version.id) in repr
        assert str(model_version.registered_model_id) in repr
        assert str(model_version.get_labels()) in repr

        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())

        model_version.log_model(classifier)
        model_version.log_artifact("coef", classifier.coef_)
        repr = str(model_version)
        assert "model" in repr
        assert "coef" in repr

    def test_get_by_client(self, client, created_entities):
        registered_model = client.set_registered_model()
        created_entities.append(registered_model)
        model_version = registered_model.get_or_create_version(name="my version")

        retrieved_model_version_by_id = client.get_registered_model_version(model_version.id)

        assert retrieved_model_version_by_id.id == model_version.id

    def test_log_environment(self, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")

        reqs = Python.read_pip_environment()
        env = Python(requirements=reqs)
        model_version.log_environment(env)

        model_version = registered_model.get_version(id=model_version.id)
        assert str(env) == str(model_version.get_environment())

        with pytest.raises(ValueError):
            model_version.log_environment(env)
        model_version.log_environment(env, overwrite=True)
        assert str(env) == str(model_version.get_environment())

    def test_del_environment(self, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")

        reqs = Python.read_pip_environment()
        env = Python(requirements=reqs)
        model_version.log_environment(env)
        model_version.del_environment()

        model_version = registered_model.get_version(id=model_version.id)
        assert not model_version.has_environment

        with pytest.raises(RuntimeError) as excinfo:
            model_version.get_environment()

        assert "environment was not previously set" in str(excinfo.value)

    def test_labels(self, client, created_entities):
        registered_model = client.set_registered_model()
        created_entities.append(registered_model)
        model_version = registered_model.get_or_create_version(name="my version")

        model_version.add_label("tag1")
        model_version.add_label("tag2")
        model_version.add_label("tag3")
        assert model_version.get_labels() == ["tag1", "tag2", "tag3"]
        model_version.del_label("tag2")
        assert model_version.get_labels() == ["tag1", "tag3"]
        model_version.del_label("tag4")
        assert model_version.get_labels() == ["tag1", "tag3"]
        model_version.add_label("tag2")
        assert model_version.get_labels() == ["tag1", "tag2", "tag3"]

        model_version.add_labels(["tag2", "tag4", "tag1", "tag5"])
        assert model_version.get_labels() == ["tag1", "tag2", "tag3", "tag4", "tag5"]

    def test_description(self, client, registered_model):
        desc = "description"
        model_version = registered_model.get_or_create_version(name="my version")
        model_version.set_description(desc)
        assert desc == model_version.get_description()

    @pytest.mark.skip(reason="functionality postponed in Client")
    def test_archive(self, model_version):
        assert (not model_version.is_archived)

        model_version.archive()
        assert model_version.is_archived

        with pytest.raises(RuntimeError) as excinfo:
            model_version.archive()

        assert "the version has already been archived" in str(excinfo.value)

    def test_clear_cache(self, registered_model):
        # Multiple log_artifacts calls, which would potentially fail without clear_cache
        model_version = registered_model.get_or_create_version(name="my version")
        model_version_2 = registered_model.get_version(id=model_version.id) # same version object

        np = pytest.importorskip("numpy")
        artifact = np.random.random((36, 12))

        for i in range(2):
            model_version.log_artifact("artifact_{}".format(2 * i), artifact)
            model_version_2.log_artifact("artifact_{}".format(2 * i + 1), artifact)

        model_version = registered_model.get_version(id=model_version.id) # re-retrieve the version
        assert len(model_version._msg.artifacts) == 4

    def test_attributes(self, client, registered_model):
        model_version = registered_model.get_or_create_version(name="my version")

        model_version.add_attribute("float-attr", 0.4)
        model_version.add_attribute("float-attr", 0.8)
        assert model_version.get_attribute("float-attr") == 0.4

        model_version.add_attribute("float-attr", 0.8, overwrite=True)
        assert model_version.get_attribute("float-attr") == 0.8
        # Test overwriting
        model_version.add_attribute("int-attr", 15)
        assert model_version.get_attribute("int-attr") == 15

        model_version.add_attributes({"int-attr": 16, "float-attr": 123.}, overwrite=True)
        assert model_version.get_attributes() == {"int-attr": 16, "float-attr": 123.}
        # Test deleting:
        model_version.del_attribute('int-attr')
        assert model_version.get_attributes() == {"float-attr": 123.}


        # Deleting non-existing key:
        model_version.del_attribute("non-existing")

    def test_patch(self, registered_model):
        NAME = "name"
        DESCRIPTION = "description"
        LABELS = ['label']
        ATTRIBUTES = {'attribute': 3}

        version = registered_model.create_version(NAME)

        version.set_description(DESCRIPTION)
        assert version.name == NAME

        version.add_labels(LABELS)
        assert version.get_description() == DESCRIPTION

        version.add_attributes(ATTRIBUTES)
        assert version.get_labels() == LABELS

        assert version.get_attributes() == ATTRIBUTES


class TestFind:
    def test_list_from_client(self, client, created_entities):
        """
        At some point, backend API was unexpectedly changed to require model ID
        in /model_versions/find, which broke client.registered_model_versions.

        """
        registered_model = client.create_registered_model()
        created_entities.append(registered_model)

        len(client.registered_model_versions)

    def test_find(self, client, created_entities):
        name = "registered_model_test"
        registered_model = client.set_registered_model()
        created_entities.append(registered_model)
        model_version = registered_model.get_or_create_version(name=name)

        find_result = registered_model.versions.find(["version == '{}'".format(name)])
        assert len(find_result) == 1
        for item in find_result:
            assert item._msg == model_version._msg

        tag_name = name + "_tag"
        versions = {name + "1": registered_model.get_or_create_version(name + "1"),
                    name + "2": registered_model.get_or_create_version(name + "2")}
        versions[name + "1"].add_label(tag_name)
        versions[name + "2"].add_label(tag_name)
        versions[name + "2"].add_label("label2")

        for version in versions:
            versions[version] = registered_model.get_version(version)

        find_result = registered_model.versions.find(["labels == \"{}\"".format(tag_name)])
        assert len(find_result) == 2
        for item in find_result:
            assert versions[item._msg.version]
            msg_other = versions[item._msg.version]._msg
            item._msg.time_updated = msg_other.time_updated = 0
            labels1 = set(item._msg.labels)
            item._msg.labels[:] = []
            labels2 = set(msg_other.labels)
            msg_other.labels[:] = []
            msg_other.model.CopyFrom(item._msg.model)
            assert labels1 == labels2
            assert item._msg == msg_other

    def test_find_stage(self, registered_model):
        # TODO: expand with other stages once client impls version transition
        assert len(registered_model.versions.find("stage == development")) == 0

        registered_model.create_version()
        assert len(registered_model.versions.find("stage == development")) == 1
        assert len(registered_model.versions.find("stage == staging")) == 0


class TestArtifacts:
    def test_log_artifact(self, model_version):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression
        key = "coef"

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        original_coef = classifier.coef_
        model_version.log_artifact(key, original_coef)

        # retrieve the artifact:
        retrieved_coef = model_version.get_artifact(key)
        assert np.array_equal(retrieved_coef, original_coef)
        artifact_msg = model_version._get_artifact_msg(key)
        assert artifact_msg.key == key
        assert artifact_msg.filename_extension == "pkl"

        # Overwrite should work:
        new_classifier = LogisticRegression()
        new_classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_artifact(key, new_classifier.coef_, overwrite=True)
        retrieved_coef = model_version.get_artifact(key)
        assert np.array_equal(retrieved_coef, new_classifier.coef_)

        # when overwrite = false, overwriting should fail
        with pytest.raises(ValueError) as excinfo:
            model_version.log_artifact(key, new_classifier.coef_)

        assert "The key has been set" in str(excinfo.value)

    def test_add_artifact_file(self, model_version, in_tempdir, random_data):
        filename = "tiny1.bin"
        FILE_CONTENTS = random_data
        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS)
        model_version.log_artifact("file", filename)

        # retrieve the artifact:
        retrieved_file = model_version.get_artifact("file")
        assert retrieved_file.getvalue() == FILE_CONTENTS

    def test_download(self, model_version, strs, in_tempdir, random_data):
        key = strs[0]
        filename = strs[1]
        new_filename = strs[2]
        FILE_CONTENTS = random_data

        # create file and upload as artifact
        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS)
        model_version.log_artifact(key, filename)
        os.remove(filename)

        # download artifact and verify contents
        new_filepath = model_version.download_artifact(key, new_filename)
        assert new_filepath == os.path.abspath(new_filename)
        with open(new_filepath, 'rb') as f:
            assert f.read() == FILE_CONTENTS

        # object as well
        obj = {'some': ["arbitrary", "object"]}
        model_version.log_artifact(key, obj, overwrite=True)
        new_filepath = model_version.download_artifact(key, new_filename)
        with open(new_filepath, 'rb') as f:
            assert pickle.load(f) == obj

    def test_download_directory(self, model_version, strs, dir_and_files, in_tempdir):
        key, download_path = strs[:2]
        dirpath, _ = dir_and_files

        model_version.log_artifact(key, dirpath)
        retrieved_path = model_version.download_artifact(key, download_path)

        # contents match
        utils.assert_dirs_match(dirpath, retrieved_path)

    def test_wrong_key(self, model_version):
        with pytest.raises(KeyError) as excinfo:
            model_version.get_model()

        assert "no model associated with this version" in str(excinfo.value)

        with pytest.raises(KeyError) as excinfo:
            model_version.get_artifact("non-existing")

        assert "no artifact found with key non-existing" in str(excinfo.value)

        np = pytest.importorskip("numpy")
        with pytest.raises(ValueError) as excinfo:
            model_version.log_artifact("model", np.random.random((36, 12)))

        assert "the key \"model\" is reserved for model; consider using log_model() instead" in str(excinfo.value)

        with pytest.raises(ValueError) as excinfo:
            model_version.del_artifact("model")

        assert "model can't be deleted through del_artifact(); consider using del_model() instead" in str(excinfo.value)

    def test_del_artifact(self, registered_model):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        model_version = registered_model.get_or_create_version(name="my version")
        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())

        model_version.log_artifact("coef", classifier.coef_)
        model_version.log_artifact("coef-2", classifier.coef_)
        model_version.log_artifact("coef-3", classifier.coef_)


        model_version.del_artifact("coef-2")
        assert len(model_version.get_artifact_keys()) == 2

        model_version.del_artifact("coef")
        assert len(model_version.get_artifact_keys()) == 1

        model_version.del_artifact("coef-3")
        assert len(model_version.get_artifact_keys()) == 0

    def test_del_model(self, registered_model):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        model_version = registered_model.get_or_create_version(name="my version")
        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_model(classifier)

        model_version = registered_model.get_version(id=model_version.id)
        assert model_version.has_model
        model_version.del_model()
        assert (not model_version.has_model)

        model_version = registered_model.get_version(id=model_version.id)
        assert (not model_version.has_model)


class TestDeployability:
    """Deployment-related functionality"""
    def test_log_model(self, model_version):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        original_coef = classifier.coef_
        model_version.log_model(classifier)

        # retrieve the classifier:
        retrieved_classfier = model_version.get_model()
        assert np.array_equal(retrieved_classfier.coef_, original_coef)

        # check model api:
        assert _artifact_utils.MODEL_API_KEY in model_version.get_artifact_keys()
        for artifact in model_version._msg.artifacts:
            if artifact.key == _artifact_utils.MODEL_API_KEY:
                assert artifact.filename_extension == "json"

        # overwrite should work:
        new_classifier = LogisticRegression()
        new_classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_model(new_classifier, overwrite=True)
        retrieved_classfier = model_version.get_model()
        assert np.array_equal(retrieved_classfier.coef_, new_classifier.coef_)

        # when overwrite = false, overwriting should fail
        with pytest.raises(ValueError) as excinfo:
            model_version.log_model(new_classifier)

        assert "model already exists" in str(excinfo.value)

        # Check custom modules:
        custom_module_filenames = {"__init__.py", "_verta_config.py"}
        for path in sys.path:
            # skip std libs and venvs
            #     This logic is from verta.client._log_modules().
            lib_python_str = os.path.join(os.sep, "lib", "python")
            i = path.find(lib_python_str)
            if i != -1 and glob.glob(os.path.join(path[:i], "bin", "python*")):
                continue

            for parent_dir, dirnames, filenames in os.walk(path):
                # only Python files
                filenames[:] = [filename for filename in filenames if filename.endswith(('.py', '.pyc', '.pyo'))]

                if not _utils.is_in_venv(path) and _utils.is_in_venv(parent_dir):
                    continue
                custom_module_filenames.update(map(os.path.basename, filenames))

        custom_modules = model_version.get_artifact(_artifact_utils.CUSTOM_MODULES_KEY)
        with zipfile.ZipFile(custom_modules, 'r') as zipf:
            assert custom_module_filenames == set(map(os.path.basename, zipf.namelist()))

    def test_download_sklearn(self, model_version, in_tempdir):
        LogisticRegression = pytest.importorskip("sklearn.linear_model").LogisticRegression

        upload_path = "model.pkl"
        download_path = "retrieved_model.pkl"

        model = LogisticRegression(C=0.67, max_iter=178)  # set some non-default values
        with open(upload_path, 'wb') as f:
            pickle.dump(model, f)

        model_version.log_model(model, custom_modules=[])
        returned_path = model_version.download_model(download_path)
        assert returned_path == os.path.abspath(download_path)

        with open(download_path, 'rb') as f:
            downloaded_model = pickle.load(f)

        assert downloaded_model.get_params() == model.get_params()

    def test_log_model_with_custom_modules(self, model_version, model_for_deployment):
        custom_modules_dir = "."

        model_version.log_model(
            model_for_deployment['model'],
            custom_modules=["."],
        )

        custom_module_filenames = {"__init__.py", "_verta_config.py"}
        for parent_dir, dirnames, filenames in os.walk(custom_modules_dir):
            # skip venvs
            #     This logic is from _utils.find_filepaths().
            exec_path_glob = os.path.join(parent_dir, "{}", "bin", "python*")
            dirnames[:] = [dirname for dirname in dirnames if not glob.glob(exec_path_glob.format(dirname))]

            custom_module_filenames.update(map(os.path.basename, filenames))

        custom_modules = model_version.get_artifact(_artifact_utils.CUSTOM_MODULES_KEY)
        with zipfile.ZipFile(custom_modules, 'r') as zipf:
            assert custom_module_filenames == set(map(os.path.basename, zipf.namelist()))

    def test_download_docker_context(self, experiment_run, model_for_deployment, in_tempdir,
                                     registered_model):
        download_to_path = "context.tgz"

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))
        model_version = registered_model.create_version_from_run(
            run_id=experiment_run.id,
            name="From Run {}".format(experiment_run.id),
        )

        filepath = model_version.download_docker_context(download_to_path)
        assert filepath == os.path.abspath(download_to_path)

        # can be loaded as tgz
        with tarfile.open(filepath, 'r:gz') as f:
            filepaths = set(f.getnames())

        assert "Dockerfile" in filepaths

    def test_fetch_artifacts(self, model_version, strs, flat_dicts):
        strs, flat_dicts = strs[:3], flat_dicts[:3]  # all 12 is excessive for a test
        for key, artifact in zip(strs, flat_dicts):
            model_version.log_artifact(key, artifact)

        try:
            artifacts = model_version.fetch_artifacts(strs)

            assert set(six.viewkeys(artifacts)) == set(strs)
            assert all(filepath.startswith(_CACHE_DIR)
                       for filepath in six.viewvalues(artifacts))

            for key, filepath in six.viewitems(artifacts):
                artifact_contents = model_version._get_artifact(key)
                with open(filepath, 'rb') as f:
                    file_contents = f.read()

                assert file_contents == artifact_contents
        finally:
            shutil.rmtree(_CACHE_DIR, ignore_errors=True)

    def test_model_artifacts(self, model_version, endpoint, in_tempdir):
        key = "foo"
        val = {'a': 1}

        class ModelWithDependency(object):
            def __init__(self, artifacts):
                with open(artifacts[key], 'rb') as f:  # should not KeyError
                    if cloudpickle.load(f) != val:
                        raise ValueError  # should not ValueError

            def predict(self, x):
                return x

        # first log junk artifact, to test `overwrite`
        bad_key = "bar"
        bad_val = {'b': 2}
        model_version.log_artifact(bad_key, bad_val)
        model_version.log_model(ModelWithDependency, custom_modules=[], artifacts=[bad_key])

        # log real artifact using `overwrite`
        model_version.log_artifact(key, val)
        model_version.log_model(ModelWithDependency, custom_modules=[], artifacts=[key], overwrite=True)
        model_version.log_environment(Python([]))

        endpoint.update(model_version, DirectUpdateStrategy(), wait=True)
        assert val == endpoint.get_deployed_model().predict(val)


class TestArbitraryModels:
    """Analogous to test_artifacts.TestArbitraryModels."""
    @staticmethod
    def _assert_no_deployment_artifacts(model_version):
        artifact_keys = model_version.get_artifact_keys()
        assert _artifact_utils.CUSTOM_MODULES_KEY not in artifact_keys
        assert _artifact_utils.MODEL_API_KEY not in artifact_keys

    def test_arbitrary_file(self, model_version, random_data):
        with tempfile.NamedTemporaryFile() as f:
            f.write(random_data)
            f.seek(0)

            model_version.log_model(f)

        assert model_version.get_model().read() == random_data

        self._assert_no_deployment_artifacts(model_version)

    def test_arbitrary_directory(self, model_version, dir_and_files):
        dirpath, filepaths = dir_and_files

        model_version.log_model(dirpath)

        with zipfile.ZipFile(model_version.get_model(), 'r') as zipf:
            assert set(zipf.namelist()) == filepaths

        self._assert_no_deployment_artifacts(model_version)

    def test_arbitrary_object(self, model_version):
        model = {'a': 1}

        model_version.log_model(model)

        assert model_version.get_model() == model

        self._assert_no_deployment_artifacts(model_version)

    def test_download_arbitrary_directory(self, model_version, dir_and_files, strs, in_tempdir):
        """Model that was originally a dir is unpacked on download."""
        dirpath, _ = dir_and_files
        download_path = strs[0]

        model_version.log_model(dirpath)
        returned_path = model_version.download_model(download_path)
        assert returned_path == os.path.abspath(download_path)

        # contents match
        utils.assert_dirs_match(dirpath, download_path)

    def test_from_run_download_arbitrary_directory(
        self,
        experiment_run,
        registered_model,
        dir_and_files,
        strs,
        in_tempdir,
    ):
        """Dir model logged to run is unpacked by model ver."""
        dirpath, _ = dir_and_files
        download_path = strs[0]

        experiment_run.log_model(dirpath)
        model_version = registered_model.create_version_from_run(
            run_id=experiment_run.id,
            name="From Run {}".format(experiment_run.id),
        )
        returned_path = model_version.download_model(download_path)
        assert returned_path == os.path.abspath(download_path)

        utils.assert_dirs_match(dirpath, download_path)

    def test_download_arbitrary_zip(self, model_version, dir_and_files, strs, in_tempdir):
        """Model that was originally a ZIP is not unpacked on download."""
        model_dir, _ = dir_and_files
        upload_path, download_path = strs[:2]

        # zip `model_dir` into `upload_path`
        with open(upload_path, 'wb') as f:
            shutil.copyfileobj(
                _artifact_utils.zip_dir(model_dir),
                f,
            )

        model_version.log_model(upload_path)
        returned_path = model_version.download_model(download_path)
        assert returned_path == os.path.abspath(download_path)

        assert zipfile.is_zipfile(download_path)
        assert filecmp.cmp(upload_path, download_path)


class TestLockLevels:
    @pytest.mark.parametrize("lock_level", (lock.Open(), lock.Redact(), lock.Closed()))
    def test_creation(self, registered_model, lock_level):
        model_ver = registered_model.create_version(lock_level=lock_level)
        assert model_ver._msg.lock_level == lock_level._as_proto()
        assert isinstance(model_ver.get_lock_level(), lock_level.__class__)

    @pytest.mark.parametrize("lock_level", (lock.Open(), lock.Redact(), lock.Closed()))
    def test_creation_from_run(self, registered_model, experiment_run, lock_level):
        model_ver = registered_model.create_version_from_run(experiment_run.id, lock_level=lock_level)
        assert model_ver._msg.lock_level == lock_level._as_proto()
        assert isinstance(model_ver.get_lock_level(), lock_level.__class__)

    def test_transition_levels(self, client, client_2, organization, created_entities):
        organization.add_member(client_2._conn.email)
        reg_model = client.create_registered_model(
            workspace=organization.name,
            visibility=visibility.OrgCustom(write=True),
        )
        created_entities.append(reg_model)
        # org owner
        admin_model_ver = reg_model.create_version()
        # same model version as R/W user
        user_model_ver = client_2.get_registered_model_version(admin_model_ver.id)

        # R/W user can upgrade lock level
        user_model_ver.set_lock_level(lock.redact)
        user_model_ver.set_lock_level(lock.closed)

        # R/W user cannot downgrade; admin can
        with pytest.raises(requests.HTTPError, match="Access Denied"):
            user_model_ver.set_lock_level(lock.redact)
        admin_model_ver.set_lock_level(lock.redact)
        with pytest.raises(requests.HTTPError, match="Access Denied"):
            user_model_ver.set_lock_level(lock.open)
        admin_model_ver.set_lock_level(lock.open)

        # admin can upgrade lock level
        admin_model_ver.set_lock_level(lock.redact)
        admin_model_ver.set_lock_level(lock.closed)

    def test_closed(self, client, client_2, organization, created_entities):
        description = "My model version"
        label = "mine"

        organization.add_member(client_2._conn.email)
        reg_model = client.create_registered_model(
            workspace=organization.name,
            visibility=visibility.OrgCustom(write=True),
        )
        created_entities.append(reg_model)

        # org owner
        admin_model_ver = reg_model.create_version(lock_level=lock.closed)
        # same model version as R/W user
        user_model_ver = client_2.get_registered_model_version(admin_model_ver.id)

        for model_ver in [admin_model_ver, user_model_ver]:
            model_ver.set_description(description)
            assert model_ver.get_description() == description
            model_ver.add_label(label)
            assert model_ver.get_labels() == [label]
            model_ver.del_label(label)
            with pytest.raises(requests.HTTPError, match="locked for changes"):
                model_ver.add_attribute("a", {"a": 1})
            with pytest.raises(requests.HTTPError, match="locked for changes"):
                model_ver.delete()

    def test_redact(self, client, client_2, organization, created_entities):
        description = "My model version"
        label = "mine"

        organization.add_member(client_2._conn.email)
        reg_model = client.create_registered_model(
            workspace=organization.name,
            visibility=visibility.OrgCustom(write=True),
        )
        created_entities.append(reg_model)

        # org owner
        admin_model_ver = reg_model.create_version(lock_level=lock.redact)
        # same model version as R/W user
        user_model_ver = client_2.get_registered_model_version(admin_model_ver.id)

        for model_ver in [admin_model_ver, user_model_ver]:
            model_ver.set_description(description)
            assert model_ver.get_description() == description
            model_ver.add_label(label)
            assert model_ver.get_labels() == [label]
            model_ver.del_label(label)

        admin_model_ver.add_attribute("a", {"a": 1})
        with pytest.raises(requests.HTTPError, match="Access Denied"):
            user_model_ver.del_attribute("a")

        admin_model_ver.log_artifact("b", {"b": 2})
        with pytest.raises(requests.HTTPError, match="Access Denied"):
            user_model_ver.del_artifact("b")

        with pytest.raises(requests.HTTPError, match="Access Denied"):
            user_model_ver.delete()
        admin_model_ver.delete()
