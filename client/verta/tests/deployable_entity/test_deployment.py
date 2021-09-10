# -*- coding: utf-8 -*-

import glob
import json
import os
import shutil
import sys
import tarfile
import tempfile
import time
import zipfile
import cloudpickle

import pytest
import six

import verta
from verta.tracking.entities._deployable_entity import _CACHE_DIR
from verta._internal_utils import (
    _artifact_utils,
    _utils,
)
from verta.endpoint.update import DirectUpdateStrategy
from verta.environment import Python


pytestmark = pytest.mark.not_oss


@pytest.fixture
def model_packaging():
    """Additional items added to model API in log_model()."""
    return {
        'python_version': _utils.get_python_version(),
        'type': "sklearn",
        'deserialization': "cloudpickle",
    }


class TestLogModel:
    def test_model(self, deployable_entity, model_for_deployment):
        deployable_entity.log_model(model_for_deployment['model'])

        assert model_for_deployment['model'].get_params() == deployable_entity.get_model().get_params()

    def test_custom_modules(self, deployable_entity, model_for_deployment):
        custom_modules_dir = "."

        deployable_entity.log_model(
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

        custom_modules = deployable_entity.get_artifact(_artifact_utils.CUSTOM_MODULES_KEY)
        with zipfile.ZipFile(custom_modules, 'r') as zipf:
            assert custom_module_filenames == set(map(os.path.basename, zipf.namelist()))

    def test_no_custom_modules(self, deployable_entity, model_for_deployment):
        deployable_entity.log_model(model_for_deployment['model'])

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

        custom_modules = deployable_entity.get_artifact(_artifact_utils.CUSTOM_MODULES_KEY)
        with zipfile.ZipFile(custom_modules, 'r') as zipf:
            assert custom_module_filenames == set(map(os.path.basename, zipf.namelist()))

    def test_model_api(self, deployable_entity, model_for_deployment, model_packaging):
        deployable_entity.log_model(
            model_for_deployment['model'],
            model_api=model_for_deployment['model_api'],
        )

        model_api = model_for_deployment['model_api'].to_dict()
        model_api.update({
            'model_packaging': model_packaging,
        })
        assert model_api == json.loads(six.ensure_str(
            deployable_entity.get_artifact(_artifact_utils.MODEL_API_KEY).read()))

    def test_no_model_api(self, deployable_entity, model_for_deployment, model_packaging):
        deployable_entity.log_model(model_for_deployment['model'])

        model_api = {
            'version': "v1",
            'model_packaging': model_packaging,
        }
        assert model_api == json.loads(six.ensure_str(
            deployable_entity.get_artifact(_artifact_utils.MODEL_API_KEY).read()))

    def test_model_class(self, deployable_entity, model_for_deployment):
        deployable_entity.log_model(model_for_deployment['model'].__class__)

        assert model_for_deployment['model'].__class__ == deployable_entity.get_model()

        retrieved_model_api = verta.utils.ModelAPI.from_file(
            deployable_entity.get_artifact(_artifact_utils.MODEL_API_KEY))
        assert retrieved_model_api.to_dict()['model_packaging']['type'] == "class"

    def test_artifacts(self, deployable_entity, model_for_deployment, strs, flat_dicts):
        for key, artifact in zip(strs, flat_dicts):
            deployable_entity.log_artifact(key, artifact)

        deployable_entity.log_model(
            model_for_deployment['model'].__class__,
            artifacts=strs,
        )

        assert deployable_entity.get_attribute("verta_model_artifacts") == strs

    def test_no_artifacts(self, deployable_entity, model_for_deployment):
        deployable_entity.log_model(model_for_deployment['model'].__class__)

        with pytest.raises(KeyError):
            deployable_entity.get_attribute("verta_model_artifacts")

    def test_wrong_type_artifacts_error(self, deployable_entity, model_for_deployment, all_values):
        # remove Nones, because they're equivalent to unprovided
        all_values = [val for val in all_values
                      if val is not None]
        # remove lists of strings and empty lists, because they're valid arguments
        all_values = [val for val in all_values
                      if not (isinstance(val, list) and all(isinstance(el, six.string_types) for el in val))]

        for val in all_values:
            with pytest.raises(TypeError):
                deployable_entity.log_model(
                    model_for_deployment['model'].__class__,
                    artifacts=val,
                )

    def test_not_class_model_artifacts_error(self, deployable_entity, model_for_deployment, strs, flat_dicts):
        for key, artifact in zip(strs, flat_dicts):
            deployable_entity.log_artifact(key, artifact)

        with pytest.raises(ValueError):
            deployable_entity.log_model(
                model_for_deployment['model'],
                artifacts=strs,
            )

    def test_unlogged_keys_artifacts_error(self, deployable_entity, model_for_deployment, strs, flat_dicts):
        with pytest.raises(ValueError):
            deployable_entity.log_model(
                model_for_deployment['model'],
                artifacts=[strs[0]],
            )

        deployable_entity.log_artifact(strs[0], flat_dicts[0])

        with pytest.raises(ValueError):
            deployable_entity.log_model(
                model_for_deployment['model'],
                artifacts=[strs[1]],
            )

        with pytest.raises(ValueError):
            deployable_entity.log_model(
                model_for_deployment['model'],
                artifacts=strs[1:],
            )

    def test_overwrite_artifacts(self, deployable_entity, endpoint, in_tempdir):
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
        deployable_entity.log_artifact(bad_key, bad_val)
        deployable_entity.log_model(ModelWithDependency, custom_modules=[], artifacts=[bad_key])

        # log real artifact using `overwrite`
        deployable_entity.log_artifact(key, val)
        deployable_entity.log_model(ModelWithDependency, custom_modules=[], artifacts=[key], overwrite=True)
        deployable_entity.log_environment(Python([]))

        endpoint.update(deployable_entity, DirectUpdateStrategy(), wait=True)
        assert val == endpoint.get_deployed_model().predict(val)

class TestFetchArtifacts:
    def test_fetch_artifacts(self, deployable_entity, strs, flat_dicts):
        strs, flat_dicts = strs[:3], flat_dicts[:3]  # all 12 is excessive for a test
        for key, artifact in zip(strs, flat_dicts):
            deployable_entity.log_artifact(key, artifact)

        try:
            artifacts = deployable_entity.fetch_artifacts(strs)

            assert set(six.viewkeys(artifacts)) == set(strs)
            assert all(filepath.startswith(_CACHE_DIR)
                       for filepath in six.viewvalues(artifacts))

            for key, filepath in six.viewitems(artifacts):
                artifact_contents, _ = deployable_entity._get_artifact(key)
                with open(filepath, 'rb') as f:
                    file_contents = f.read()

                assert file_contents == artifact_contents
        finally:
            shutil.rmtree(_CACHE_DIR, ignore_errors=True)

    def test_cached_fetch_artifacts(self, deployable_entity, strs, flat_dicts):
        key = strs[0]

        deployable_entity.log_artifact(key, flat_dicts[0])

        try:
            filepath = deployable_entity.fetch_artifacts([key])[key]
            last_modified = os.path.getmtime(filepath)

            time.sleep(3)
            assert deployable_entity.fetch_artifacts([key])[key] == filepath

            assert os.path.getmtime(filepath) == last_modified
        finally:
            shutil.rmtree(_CACHE_DIR, ignore_errors=True)

    def test_fetch_zip(self, deployable_entity, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        deployable_entity.log_artifact(key, dirpath)

        try:
            dirpath = deployable_entity.fetch_artifacts([key])[key]

            assert dirpath.startswith(_CACHE_DIR)

            retrieved_filepaths = set()
            for root, _, files in os.walk(dirpath):
                for filename in files:
                    filepath = os.path.join(root, filename)
                    filepath = os.path.relpath(filepath, dirpath)
                    retrieved_filepaths.add(filepath)

            assert filepaths == retrieved_filepaths
        finally:
            shutil.rmtree(_CACHE_DIR, ignore_errors=True)

    def test_cached_fetch_zip(self, deployable_entity, strs, dir_and_files):
        dirpath, _ = dir_and_files
        key = strs[0]

        deployable_entity.log_artifact(key, dirpath)

        try:
            dirpath = deployable_entity.fetch_artifacts([key])[key]
            last_modified = os.path.getmtime(dirpath)

            time.sleep(3)
            assert deployable_entity.fetch_artifacts([key])[key] == dirpath

            assert os.path.getmtime(dirpath) == last_modified
        finally:
            shutil.rmtree(_CACHE_DIR, ignore_errors=True)

    def test_fetch_tgz(self, deployable_entity, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        with tempfile.NamedTemporaryFile(suffix='.tgz') as tempf:
            # make archive
            with tarfile.open(tempf.name, 'w:gz') as tarf:
                tarf.add(dirpath, "")
            tempf.flush()  # flush object buffer
            os.fsync(tempf.fileno())  # flush OS buffer
            tempf.seek(0)

            deployable_entity.log_artifact(key, tempf.name)

        try:
            dirpath = deployable_entity.fetch_artifacts([key])[key]

            assert dirpath.startswith(_CACHE_DIR)

            retrieved_filepaths = set()
            for root, _, files in os.walk(dirpath):
                for filename in files:
                    filepath = os.path.join(root, filename)
                    filepath = os.path.relpath(filepath, dirpath)
                    retrieved_filepaths.add(filepath)

            assert filepaths == retrieved_filepaths
        finally:
            shutil.rmtree(_CACHE_DIR, ignore_errors=True)

    def test_fetch_tar(self, deployable_entity, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        with tempfile.NamedTemporaryFile(suffix='.tar') as tempf:
            # make archive
            with tarfile.open(tempf.name, 'w') as tarf:
                tarf.add(dirpath, "")
            tempf.flush()  # flush object buffer
            os.fsync(tempf.fileno())  # flush OS buffer
            tempf.seek(0)

            deployable_entity.log_artifact(key, tempf.name)

        try:
            dirpath = deployable_entity.fetch_artifacts([key])[key]

            assert dirpath.startswith(_CACHE_DIR)

            retrieved_filepaths = set()
            for root, _, files in os.walk(dirpath):
                for filename in files:
                    filepath = os.path.join(root, filename)
                    filepath = os.path.relpath(filepath, dirpath)
                    retrieved_filepaths.add(filepath)

            assert filepaths == retrieved_filepaths
        finally:
            shutil.rmtree(_CACHE_DIR, ignore_errors=True)

    def test_fetch_tar_gz(self, deployable_entity, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        with tempfile.NamedTemporaryFile(suffix='.tar.gz') as tempf:
            # make archive
            with tarfile.open(tempf.name, 'w:gz') as tarf:
                tarf.add(dirpath, "")
            tempf.flush()  # flush object buffer
            os.fsync(tempf.fileno())  # flush OS buffer
            tempf.seek(0)

            deployable_entity.log_artifact(key, tempf.name)

        try:
            dirpath = deployable_entity.fetch_artifacts([key])[key]

            assert dirpath.startswith(_CACHE_DIR)

            retrieved_filepaths = set()
            for root, _, files in os.walk(dirpath):
                for filename in files:
                    filepath = os.path.join(root, filename)
                    filepath = os.path.relpath(filepath, dirpath)
                    retrieved_filepaths.add(filepath)

            assert filepaths == retrieved_filepaths
        finally:
            shutil.rmtree(_CACHE_DIR, ignore_errors=True)

    def test_wrong_type_artifacts_error(self, deployable_entity, all_values):
        # remove lists of strings and empty lists, because they're valid arguments
        all_values = [val for val in all_values
                      if not (isinstance(val, list) and all(isinstance(el, six.string_types) for el in val))]

        for val in all_values:
            with pytest.raises(TypeError):
                deployable_entity.fetch_artifacts(val)

    def test_unlogged_keys_artifacts_error(self, deployable_entity, strs, flat_dicts):
        with pytest.raises(ValueError):
            deployable_entity.fetch_artifacts([strs[0]])

        deployable_entity.log_artifact(strs[0], flat_dicts[0])

        with pytest.raises(ValueError):
            deployable_entity.fetch_artifacts([strs[1]])

        with pytest.raises(ValueError):
            deployable_entity.fetch_artifacts(strs[1:])


class TestDeployability:
    """Deployment-related functionality"""

    def test_log_environment(self, registered_model):
        deployable_entity = registered_model.get_or_create_version(name="my version")

        reqs = Python.read_pip_environment()
        env = Python(requirements=reqs)
        deployable_entity.log_environment(env)

        deployable_entity = registered_model.get_version(id=deployable_entity.id)
        assert str(env) == str(deployable_entity.get_environment())

        with pytest.raises(ValueError):
            deployable_entity.log_environment(env)
        deployable_entity.log_environment(env, overwrite=True)
        assert str(env) == str(deployable_entity.get_environment())

    def test_del_environment(self, registered_model):
        deployable_entity = registered_model.get_or_create_version(name="my version")

        reqs = Python.read_pip_environment()
        env = Python(requirements=reqs)
        deployable_entity.log_environment(env)
        deployable_entity.del_environment()

        deployable_entity = registered_model.get_version(id=deployable_entity.id)
        assert not deployable_entity.has_environment

        with pytest.raises(RuntimeError) as excinfo:
            deployable_entity.get_environment()

        assert "environment was not previously set" in str(excinfo.value)

    def test_log_model(self, deployable_entity):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        original_coef = classifier.coef_
        deployable_entity.log_model(classifier)

        # retrieve the classifier:
        retrieved_classfier = deployable_entity.get_model()
        assert np.array_equal(retrieved_classfier.coef_, original_coef)

        # check model api:
        assert _artifact_utils.MODEL_API_KEY in deployable_entity.get_artifact_keys()
        for artifact in deployable_entity._msg.artifacts:
            if artifact.key == _artifact_utils.MODEL_API_KEY:
                assert artifact.filename_extension == "json"

        # overwrite should work:
        new_classifier = LogisticRegression()
        new_classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        deployable_entity.log_model(new_classifier, overwrite=True)
        retrieved_classfier = deployable_entity.get_model()
        assert np.array_equal(retrieved_classfier.coef_, new_classifier.coef_)

        # when overwrite = false, overwriting should fail
        with pytest.raises(ValueError) as excinfo:
            deployable_entity.log_model(new_classifier)

        assert "already exists" in str(excinfo.value)

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
                filenames[:] = [
                    filename
                    for filename in filenames
                    if filename.endswith((".py", ".pyc", ".pyo"))
                ]

                if not _utils.is_in_venv(path) and _utils.is_in_venv(parent_dir):
                    continue
                custom_module_filenames.update(map(os.path.basename, filenames))

        custom_modules = deployable_entity.get_artifact(_artifact_utils.CUSTOM_MODULES_KEY)
        with zipfile.ZipFile(custom_modules, "r") as zipf:
            assert custom_module_filenames == set(
                map(os.path.basename, zipf.namelist())
            )

    def test_download_sklearn(self, deployable_entity, in_tempdir):
        LogisticRegression = pytest.importorskip(
            "sklearn.linear_model"
        ).LogisticRegression

        upload_path = "model.pkl"
        download_path = "retrieved_model.pkl"

        model = LogisticRegression(C=0.67, max_iter=178)  # set some non-default values
        with open(upload_path, "wb") as f:
            pickle.dump(model, f)

        deployable_entity.log_model(model, custom_modules=[])
        returned_path = deployable_entity.download_model(download_path)
        assert returned_path == os.path.abspath(download_path)

        with open(download_path, "rb") as f:
            downloaded_model = pickle.load(f)

        assert downloaded_model.get_params() == model.get_params()

    def test_log_model_with_custom_modules(self, deployable_entity, model_for_deployment):
        custom_modules_dir = "."

        deployable_entity.log_model(
            model_for_deployment["model"],
            custom_modules=["."],
        )

        custom_module_filenames = {"__init__.py", "_verta_config.py"}
        for parent_dir, dirnames, filenames in os.walk(custom_modules_dir):
            # skip venvs
            #     This logic is from _utils.find_filepaths().
            exec_path_glob = os.path.join(parent_dir, "{}", "bin", "python*")
            dirnames[:] = [
                dirname
                for dirname in dirnames
                if not glob.glob(exec_path_glob.format(dirname))
            ]

            custom_module_filenames.update(map(os.path.basename, filenames))

        custom_modules = deployable_entity.get_artifact(_artifact_utils.CUSTOM_MODULES_KEY)
        with zipfile.ZipFile(custom_modules, "r") as zipf:
            assert custom_module_filenames == set(
                map(os.path.basename, zipf.namelist())
            )

    def test_download_docker_context(
        self, deployable_entity, model_for_deployment, in_tempdir, registered_model
    ):
        download_to_path = "context.tgz"

        deployable_entity.log_model(model_for_deployment["model"], custom_modules=[])
        deployable_entity.log_environment(Python(["scikit-learn"]))
        deployable_entity = registered_model.create_version_from_run(
            run_id=deployable_entity.id,
            name="From Run {}".format(deployable_entity.id),
        )

        filepath = deployable_entity.download_docker_context(download_to_path)
        assert filepath == os.path.abspath(download_to_path)

        # can be loaded as tgz
        with tarfile.open(filepath, "r:gz") as f:
            filepaths = set(f.getnames())

        assert "Dockerfile" in filepaths

    def test_fetch_artifacts(self, deployable_entity, strs, flat_dicts):
        strs, flat_dicts = strs[:3], flat_dicts[:3]  # all 12 is excessive for a test
        for key, artifact in zip(strs, flat_dicts):
            deployable_entity.log_artifact(key, artifact)

        try:
            artifacts = deployable_entity.fetch_artifacts(strs)

            assert set(six.viewkeys(artifacts)) == set(strs)
            assert all(
                filepath.startswith(_deployable_entity._CACHE_DIR)
                for filepath in six.viewvalues(artifacts)
            )

            for key, filepath in six.viewitems(artifacts):
                artifact_contents = deployable_entity._get_artifact(key)
                with open(filepath, "rb") as f:
                    file_contents = f.read()

                assert file_contents == artifact_contents
        finally:
            shutil.rmtree(_deployable_entity._CACHE_DIR, ignore_errors=True)

    def test_model_artifacts(self, deployable_entity, endpoint, in_tempdir):
        key = "foo"
        val = {"a": 1}

        class ModelWithDependency(object):
            def __init__(self, artifacts):
                with open(artifacts[key], "rb") as f:  # should not KeyError
                    if cloudpickle.load(f) != val:
                        raise ValueError  # should not ValueError

            def predict(self, x):
                return x

        # first log junk artifact, to test `overwrite`
        bad_key = "bar"
        bad_val = {"b": 2}
        deployable_entity.log_artifact(bad_key, bad_val)
        deployable_entity.log_model(
            ModelWithDependency, custom_modules=[], artifacts=[bad_key]
        )

        # log real artifact using `overwrite`
        deployable_entity.log_artifact(key, val)
        deployable_entity.log_model(
            ModelWithDependency, custom_modules=[], artifacts=[key], overwrite=True
        )
        deployable_entity.log_environment(Python([]))

        endpoint.update(deployable_entity, DirectUpdateStrategy(), wait=True)
        assert val == endpoint.get_deployed_model().predict(val)
