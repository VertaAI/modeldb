# -*- coding: utf-8 -*-

import glob
import json
import os
import pickle
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


class TestLogModel:
    def test_model(self, deployable_entity, model_for_deployment):
        deployable_entity.log_model(model_for_deployment["model"])

        assert (
            model_for_deployment["model"].get_params()
            == deployable_entity.get_model().get_params()
        )

    def test_custom_modules(self, deployable_entity, model_for_deployment):
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

        custom_modules = deployable_entity.get_artifact(
            _artifact_utils.CUSTOM_MODULES_KEY
        )
        with zipfile.ZipFile(custom_modules, "r") as zipf:
            assert custom_module_filenames == set(
                map(os.path.basename, zipf.namelist())
            )

    def test_no_custom_modules(self, deployable_entity, model_for_deployment):
        deployable_entity.log_model(model_for_deployment["model"])

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

        custom_modules = deployable_entity.get_artifact(
            _artifact_utils.CUSTOM_MODULES_KEY
        )
        with zipfile.ZipFile(custom_modules, "r") as zipf:
            assert custom_module_filenames == set(
                map(os.path.basename, zipf.namelist())
            )

    def test_model_api(self, deployable_entity, model_for_deployment):
        deployable_entity.log_model(
            model_for_deployment["model"],
            model_api=model_for_deployment["model_api"],
        )

        model_api = model_for_deployment["model_api"].to_dict()
        assert model_api == json.loads(
            six.ensure_str(
                deployable_entity.get_artifact(_artifact_utils.MODEL_API_KEY).read()
            )
        )

    def test_no_model_api(self, deployable_entity, model_for_deployment):
        deployable_entity.log_model(model_for_deployment["model"])

        model_api = {
            "version": "v1",
        }
        assert model_api == json.loads(
            six.ensure_str(
                deployable_entity.get_artifact(_artifact_utils.MODEL_API_KEY).read()
            )
        )

    def test_model_class(self, deployable_entity, model_for_deployment):
        deployable_entity.log_model(model_for_deployment["model"].__class__)

        assert model_for_deployment["model"].__class__ == deployable_entity.get_model()

    def test_artifacts(self, deployable_entity, model_for_deployment, strs, flat_dicts):
        for key, artifact in zip(strs, flat_dicts):
            deployable_entity.log_artifact(key, artifact)

        deployable_entity.log_model(
            model_for_deployment["model"].__class__,
            artifacts=strs,
        )

        assert deployable_entity.get_attribute("verta_model_artifacts") == strs

    def test_no_artifacts(self, deployable_entity, model_for_deployment):
        deployable_entity.log_model(model_for_deployment["model"].__class__)

        with pytest.raises(KeyError):
            deployable_entity.get_attribute("verta_model_artifacts")

    def test_wrong_type_artifacts_error(
        self, deployable_entity, model_for_deployment, all_values
    ):
        # remove Nones, because they're equivalent to unprovided
        all_values = [val for val in all_values if val is not None]
        # remove lists of strings and empty lists, because they're valid arguments
        all_values = [
            val
            for val in all_values
            if not (
                isinstance(val, list)
                and all(isinstance(el, six.string_types) for el in val)
            )
        ]

        for val in all_values:
            with pytest.raises(TypeError):
                deployable_entity.log_model(
                    model_for_deployment["model"].__class__,
                    artifacts=val,
                )

    def test_not_class_model_artifacts_error(
        self, deployable_entity, model_for_deployment, strs, flat_dicts
    ):
        for key, artifact in zip(strs, flat_dicts):
            deployable_entity.log_artifact(key, artifact)

        with pytest.raises(ValueError):
            deployable_entity.log_model(
                model_for_deployment["model"],
                artifacts=strs,
            )

    def test_unlogged_keys_artifacts_error(
        self, deployable_entity, model_for_deployment, strs, flat_dicts
    ):
        with pytest.raises(ValueError):
            deployable_entity.log_model(
                model_for_deployment["model"],
                artifacts=[strs[0]],
            )

        deployable_entity.log_artifact(strs[0], flat_dicts[0])

        with pytest.raises(ValueError):
            deployable_entity.log_model(
                model_for_deployment["model"],
                artifacts=[strs[1]],
            )

        with pytest.raises(ValueError):
            deployable_entity.log_model(
                model_for_deployment["model"],
                artifacts=strs[1:],
            )

    @pytest.mark.deployment
    def test_overwrite_artifacts(self, deployable_entity, endpoint, in_tempdir):
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


class TestFetchArtifacts:
    def test_fetch_artifacts(self, deployable_entity, strs, flat_dicts):
        strs, flat_dicts = strs[:3], flat_dicts[:3]  # all 12 is excessive for a test
        for key, artifact in zip(strs, flat_dicts):
            deployable_entity.log_artifact(key, artifact)

        artifacts = deployable_entity.fetch_artifacts(strs)

        assert set(six.viewkeys(artifacts)) == set(strs)
        assert all(
            filepath.startswith(_CACHE_DIR) for filepath in six.viewvalues(artifacts)
        )

        for key, filepath in six.viewitems(artifacts):
            artifact_contents = deployable_entity._get_artifact(key)
            if type(artifact_contents) is tuple:
                # ER returns (contents, path_only)
                # TODO: ER & RMV _get_artifact() should return the same thing
                artifact_contents, _ = artifact_contents

            with open(filepath, "rb") as f:
                file_contents = f.read()

            assert file_contents == artifact_contents

    def test_cached_fetch_artifacts(self, deployable_entity, strs, flat_dicts):
        key = strs[0]

        deployable_entity.log_artifact(key, flat_dicts[0])

        filepath = deployable_entity.fetch_artifacts([key])[key]
        last_modified = os.path.getmtime(filepath)

        time.sleep(3)
        assert deployable_entity.fetch_artifacts([key])[key] == filepath

        assert os.path.getmtime(filepath) == last_modified

    def test_fetch_zip(self, deployable_entity, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        deployable_entity.log_artifact(key, dirpath)

        dirpath = deployable_entity.fetch_artifacts([key])[key]

        assert dirpath.startswith(_CACHE_DIR)

        retrieved_filepaths = set()
        for root, _, files in os.walk(dirpath):
            for filename in files:
                filepath = os.path.join(root, filename)
                filepath = os.path.relpath(filepath, dirpath)
                retrieved_filepaths.add(filepath)

        assert filepaths == retrieved_filepaths

    def test_cached_fetch_zip(self, deployable_entity, strs, dir_and_files):
        dirpath, _ = dir_and_files
        key = strs[0]

        deployable_entity.log_artifact(key, dirpath)

        dirpath = deployable_entity.fetch_artifacts([key])[key]
        last_modified = os.path.getmtime(dirpath)

        time.sleep(3)
        assert deployable_entity.fetch_artifacts([key])[key] == dirpath

        assert os.path.getmtime(dirpath) == last_modified

    def test_fetch_tgz(self, deployable_entity, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        with tempfile.NamedTemporaryFile(suffix=".tgz") as tempf:
            # make archive
            with tarfile.open(tempf.name, "w:gz") as tarf:
                tarf.add(dirpath, "")
            tempf.flush()  # flush object buffer
            os.fsync(tempf.fileno())  # flush OS buffer
            tempf.seek(0)

            deployable_entity.log_artifact(key, tempf.name)

        dirpath = deployable_entity.fetch_artifacts([key])[key]

        assert dirpath.startswith(_CACHE_DIR)

        retrieved_filepaths = set()
        for root, _, files in os.walk(dirpath):
            for filename in files:
                filepath = os.path.join(root, filename)
                filepath = os.path.relpath(filepath, dirpath)
                retrieved_filepaths.add(filepath)

        assert filepaths == retrieved_filepaths

    def test_fetch_tar(self, deployable_entity, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        with tempfile.NamedTemporaryFile(suffix=".tar") as tempf:
            # make archive
            with tarfile.open(tempf.name, "w") as tarf:
                tarf.add(dirpath, "")
            tempf.flush()  # flush object buffer
            os.fsync(tempf.fileno())  # flush OS buffer
            tempf.seek(0)

            deployable_entity.log_artifact(key, tempf.name)

        dirpath = deployable_entity.fetch_artifacts([key])[key]

        assert dirpath.startswith(_CACHE_DIR)

        retrieved_filepaths = set()
        for root, _, files in os.walk(dirpath):
            for filename in files:
                filepath = os.path.join(root, filename)
                filepath = os.path.relpath(filepath, dirpath)
                retrieved_filepaths.add(filepath)

        assert filepaths == retrieved_filepaths

    def test_fetch_tar_gz(self, deployable_entity, strs, dir_and_files):
        dirpath, filepaths = dir_and_files
        key = strs[0]

        with tempfile.NamedTemporaryFile(suffix=".tar.gz") as tempf:
            # make archive
            with tarfile.open(tempf.name, "w:gz") as tarf:
                tarf.add(dirpath, "")
            tempf.flush()  # flush object buffer
            os.fsync(tempf.fileno())  # flush OS buffer
            tempf.seek(0)

            deployable_entity.log_artifact(key, tempf.name)

        dirpath = deployable_entity.fetch_artifacts([key])[key]

        assert dirpath.startswith(_CACHE_DIR)

        retrieved_filepaths = set()
        for root, _, files in os.walk(dirpath):
            for filename in files:
                filepath = os.path.join(root, filename)
                filepath = os.path.relpath(filepath, dirpath)
                retrieved_filepaths.add(filepath)

        assert filepaths == retrieved_filepaths

    def test_wrong_type_artifacts_error(self, deployable_entity, all_values):
        # remove lists of strings and empty lists, because they're valid arguments
        all_values = [
            val
            for val in all_values
            if not (
                isinstance(val, list)
                and all(isinstance(el, six.string_types) for el in val)
            )
        ]

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


class TestEnvironment:
    def test_log_environment(self, deployable_entity, environment):
        deployable_entity.log_environment(environment)
        assert environment == deployable_entity.get_environment()

    def test_overwrite_environment(self, deployable_entity, environment):
        deployable_entity.log_environment(environment)

        with pytest.raises(ValueError, match="environment already exists"):
            deployable_entity.log_environment(environment)

        deployable_entity.log_environment(environment, overwrite=True)
        assert environment == deployable_entity.get_environment()


class TestDeployability:
    """Deployment-related functionality"""

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

        custom_modules = deployable_entity.get_artifact(
            _artifact_utils.CUSTOM_MODULES_KEY
        )
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

    @pytest.mark.deployment
    def test_download_docker_context(
        self, deployable_entity, model_for_deployment, in_tempdir, registered_model
    ):
        download_to_path = "context.tgz"

        deployable_entity.log_model(model_for_deployment["model"], custom_modules=[])
        deployable_entity.log_environment(Python(["scikit-learn"]))

        filepath = deployable_entity.download_docker_context(download_to_path)
        assert filepath == os.path.abspath(download_to_path)

        # can be loaded as tgz
        with tarfile.open(filepath, "r:gz") as f:
            filepaths = set(f.getnames())

        assert "Dockerfile" in filepaths

    def test_fetch_artifacts(self, model_version, strs, flat_dicts):
        strs, flat_dicts = strs[:3], flat_dicts[:3]  # all 12 is excessive for a test
        for key, artifact in zip(strs, flat_dicts):
            model_version.log_artifact(key, artifact)

        artifacts = model_version.fetch_artifacts(strs)

        assert set(six.viewkeys(artifacts)) == set(strs)
        assert all(
            filepath.startswith(_CACHE_DIR) for filepath in six.viewvalues(artifacts)
        )

        for key, filepath in six.viewitems(artifacts):
            artifact_contents = model_version._get_artifact(key)
            with open(filepath, "rb") as f:
                file_contents = f.read()

            assert file_contents == artifact_contents

    @pytest.mark.deployment
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
