# -*- coding: utf-8 -*-

import json
import os
import zipfile

import hypothesis
import pytest

from verta.tracking.entities._deployable_entity import _DeployableEntity
from verta._internal_utils.custom_modules import CustomModules

from .. import strategies, utils
from . import contexts

# also deployable_entity/test_deployment.py::TestLogModel::test_custom_modules


class TestCollection:
    @hypothesis.settings(deadline=None)
    @hypothesis.given(name=strategies.python_module_name())  # pylint: disable=no-value-for-parameter
    def test_module_and_local_dir_have_same_name(self, name, make_tempdir):
        # avoid using an existing package name
        hypothesis.assume(not CustomModules.is_importable(name))

        with utils.chtempdir():
            # create local directory with same name as package
            local_dir = os.path.abspath(name)
            os.mkdir(local_dir)
            with open(os.path.join(local_dir, "empty.json"), "w") as f:
                json.dump({}, f)

            # create package in another directory and install
            with contexts.installable_package(name, dir=str(make_tempdir())) as pkg_dir:
                with contexts.installed_local_package(pkg_dir, name):
                    # collect and extract custom modules
                    custom_modules = _DeployableEntity._custom_modules_as_artifact([name])
                    custom_modules_dir = os.path.abspath("custom_modules")
                    with zipfile.ZipFile(custom_modules, "r") as zipf:
                        zipf.extractall(custom_modules_dir)

                    utils.assert_dirs_match(
                        os.path.join(custom_modules_dir, name),
                        local_dir,  # TODO: this is incorrect
                    )

    @hypothesis.settings(deadline=None)
    @hypothesis.given(name=strategies.python_module_name())  # pylint: disable=no-value-for-parameter
    def test_module_and_local_pkg_have_same_name(self, name):
        """A specific case of :meth:`test_module_and_local_dir_have_same_name`.

        The local package is *not* directly ``import``able because it is
        nested one level in, but the root directory still bears its name.

        """
        # avoid using an existing package name
        hypothesis.assume(not CustomModules.is_importable(name))

        with utils.chtempdir():
            # create package in *current* directory and install
            with contexts.installable_package(name, dir=".") as pkg_dir:
                with contexts.installed_local_package(pkg_dir, name):
                    # collect and extract custom modules
                    custom_modules = _DeployableEntity._custom_modules_as_artifact([name])
                    custom_modules_dir = os.path.abspath("custom_modules")
                    with zipfile.ZipFile(custom_modules, "r") as zipf:
                        zipf.extractall(custom_modules_dir)

                    utils.assert_dirs_match(
                        os.path.join(custom_modules_dir, name),
                        pkg_dir,  # TODO: this is incorrect
                    )
