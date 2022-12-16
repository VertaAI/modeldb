# -*- coding: utf-8 -*-

import datetime
import json
import os
import tarfile
import uuid

import hypothesis
import hypothesis.strategies as st
import pytest

from verta._internal_utils import _utils
from verta.data_types import _verta_data_type
from verta.environment import Python
from verta.registry.entities import RegisteredModelVersion
from verta.tracking.entities import _deployable_entity

from ... import utils


pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


@pytest.mark.deployment
class TestDeployability:
    def test_from_run_download_docker_context(
        self, experiment_run, model_for_deployment, in_tempdir, registered_model
    ):
        """deployable_entity/test_deployment.py::TestDeployability::test_download_docker_context

        But through create_version_from_run().

        """
        download_to_path = "context.tgz"

        experiment_run.log_model(model_for_deployment["model"], custom_modules=[])
        experiment_run.log_environment(Python(["scikit-learn"]))
        model_version = registered_model.create_version_from_run(
            run_id=experiment_run.id,
            name="From Run {}".format(experiment_run.id),
        )

        filepath = model_version.download_docker_context(download_to_path)
        assert filepath == os.path.abspath(download_to_path)

        # can be loaded as tgz
        with tarfile.open(filepath, "r:gz") as f:
            filepaths = set(f.getnames())

        assert "Dockerfile" in filepaths


class TestArbitraryModels:
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


class TestEnvironment:
    def test_del_environment(self, model_version, environment):
        model_version.log_environment(environment)

        model_version.del_environment()
        assert not model_version.has_environment

        with pytest.raises(
            RuntimeError,
            match="environment was not previously set",
        ):
            model_version.get_environment()
