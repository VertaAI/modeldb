# -*- coding: utf-8 -*-

import importlib

import hypothesis
import pytest

from verta.environment import Python
from verta._internal_utils.custom_modules import CustomModules

from ... import utils
from .. import contexts, models


pytestmark = [pytest.mark.integration]


@pytest.mark.deployment
class TestPipInstalledModule:
    def test_deploy_pip_installed_custom_modules(
        self,
        deployable_entity,
        endpoint,
        worker_id,
    ):
        """A pip-installed custom module can be specified by name (instead of path).

        A success means that custom modules successfully collected the
        pip-installed module, and the deployed model could import it.

        """
        name = worker_id

        # avoid using an existing package nam
        hypothesis.assume(not CustomModules.is_importable(name))

        with utils.tempdir() as tempd:
            # create package in another directory and install
            with contexts.installable_package(name, dir=tempd) as pkg_dir:
                with contexts.installed_local_package(pkg_dir, name):
                    Model = models.create_custom_module_model(name)
                    assert Model().predict("") == name  # sanity check

                    deployable_entity.log_model(Model, custom_modules=[name])
        deployable_entity.log_environment(Python([]))

        endpoint.update(deployable_entity, wait=True)

        assert endpoint.get_deployed_model().predict("") == name

    def test_deploy_module_and_local_pkg_have_same_name(
        self,
        deployable_entity,
        endpoint,
        worker_id,
    ):
        """Deployment analogue to ``custom_modules/test_custom_modules.py::TestPipInstalledModule::test_module_and_local_pkg_have_same_name``.

        A success means that custom modules successfully collected the
        pip-installed module, and the deployed model could import it.

        """
        name = worker_id

        # avoid using an existing package name
        hypothesis.assume(not CustomModules.is_importable(name))

        with utils.chtempdir():
            # create package in *current* directory and install
            with contexts.installable_package(name, dir=".") as pkg_dir:
                with contexts.installed_local_package(pkg_dir, name):
                    Model = models.create_custom_module_model(name)
                    assert Model().predict("") == name  # sanity check

                    deployable_entity.log_model(Model, custom_modules=[name])
        deployable_entity.log_environment(Python([]))

        endpoint.update(deployable_entity, wait=True)

        assert endpoint.get_deployed_model().predict("") == name
