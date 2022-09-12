# -*- coding: utf-8 -*-

import filecmp
import json
import os
import pkgutil
import zipfile

import hypothesis
import pytest

from verta.tracking.entities._deployable_entity import _DeployableEntity
from verta._internal_utils.custom_modules import CustomModules

from .. import utils
from . import contexts


class TestPipInstalledModule:
    @staticmethod
    def assert_in_custom_modules(custom_modules, module_name):
        module = CustomModules.get_module_path(module_name)

        with utils.tempdir() as custom_modules_dir:
            with zipfile.ZipFile(custom_modules, "r") as zipf:
                zipf.extractall(custom_modules_dir)

            # TODO: extract sys.path from _verta_config.py instead of walking
            for parent_dir, dirnames, filenames in os.walk(custom_modules_dir):
                if os.path.basename(module) in dirnames + filenames:
                    retrieved_module = os.path.join(
                        parent_dir,
                        os.path.basename(module),
                    )
                    break
            else:
                raise ValueError("module not found in custom modules")

            if os.path.isfile(module):
                assert filecmp.cmp(module, retrieved_module)
            else:
                utils.assert_dirs_match(module, retrieved_module)

    @pytest.mark.parametrize(
        "name",
        sorted(module[1] for module in pkgutil.iter_modules()),
    )
    def test_module(self, name):
        """pip-installed module can be collected."""
        if name == "tests" or name == "conftest" or name.startswith("test_"):
            pytest.skip(
                "pytest modifies both import mechanisms and module objects,"
                " which we can't handle right now"
            )
        if CustomModules.get_module_path(name) in ("built-in", "frozen"):
            pytest.skip("built into Python; no module file to collect")

        custom_modules = _DeployableEntity._custom_modules_as_artifact([name])
        self.assert_in_custom_modules(custom_modules, name)

    @pytest.mark.parametrize(
        "names",
        [
            ["cloudpickle", "hypothesis"],
            ["cloudpickle", "hypothesis", "pytest"],
        ],
    )
    def test_multiple_modules(self, names):
        """Multiple pip-installed modules can be collected at once."""
        custom_modules = _DeployableEntity._custom_modules_as_artifact(names)
        for name in names:
            self.assert_in_custom_modules(custom_modules, name)

    def test_module_and_local_dir_have_same_name(self, worker_id):
        """If a pip-installed module and a local directory share a name, the module is collected.

        If a user can import a package "foo" in their environment, and uses
        custom modules to find "foo", we will prefer that package over a
        directory/file "foo" in the cwd. Otherwise, it is very difficult or
        impossible to force the installed package.

        """
        name = worker_id

        # avoid using an existing package name
        hypothesis.assume(not CustomModules.is_importable(name))

        with utils.chtempdir():
            # create local directory with same name as package
            local_dir = os.path.abspath(name)
            os.mkdir(local_dir)
            with open(os.path.join(local_dir, "empty.json"), "w") as f:
                json.dump({}, f)

            # create package in another directory and install
            with utils.tempdir() as tempd:
                with contexts.installable_package(name, dir=tempd) as pkg_dir:
                    with contexts.installed_local_package(pkg_dir, name):
                        # collect and validate custom modules
                        custom_modules = _DeployableEntity._custom_modules_as_artifact(
                            [name],
                        )
                        self.assert_in_custom_modules(custom_modules, name)

    def test_module_and_local_pkg_have_same_name(self, worker_id):
        """A specific case of :meth:`test_module_and_local_dir_have_same_name`.

        The local directory *is* a Python package repository (but not directly
        importable without ``cd``ing one level into it).

        A user may have a monolithic project with model management scripts
        alongside Python package directories (that may *also* be installed
        into the environment).

        """
        name = worker_id

        # avoid using an existing package name
        hypothesis.assume(not CustomModules.is_importable(name))

        with utils.chtempdir():
            # create package in *current* directory and install
            with contexts.installable_package(name, dir=".") as pkg_dir:
                with contexts.installed_local_package(pkg_dir, name):
                    # collect and validate custom modules
                    custom_modules = _DeployableEntity._custom_modules_as_artifact(
                        [name],
                    )
                    self.assert_in_custom_modules(custom_modules, name)
