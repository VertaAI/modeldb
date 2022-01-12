# -*- coding: utf-8 -*-

import os
import shutil
import subprocess
import sys
import tempfile
import textwrap

import hypothesis
import pytest

from .. import constants
from .. import utils


@pytest.fixture(scope="class")
def in_fake_venv(tempdir_root):
    """Move test to execute inside a mocked, empty virtual environment."""
    dirpath = tempfile.mkdtemp(dir=tempdir_root)

    # empty site-packages/
    os.makedirs(os.path.join(dirpath, constants.LIB_SITE_PACKAGES))
    os.makedirs(os.path.join(dirpath, constants.LIB32_SITE_PACKAGES))
    os.makedirs(os.path.join(dirpath, constants.LIB64_SITE_PACKAGES))

    # empty __pycache__/
    os.makedirs(os.path.join(dirpath, constants.BIN_PYCACHE))

    # fake Python binary
    open(os.path.join(dirpath, "bin", constants.PYTHON_VERSION_SEGMENT), "wb").close()

    try:
        with utils.chdir(dirpath):
            yield dirpath
    finally:
        shutil.rmtree(dirpath)


@pytest.fixture()
def make_package():
    """Make simple pip-installable packages.

    Warnings
    --------
    Packages should not be moved in the filesystem after they are created,
    otherwise teardown will be unable to delete them.

    Notes
    -----
    In the spirit of general convention, the package and its root module have
    the same name. This isn't required nor universal however, and they have
    divergent character restrictions.

    """
    created_packages = []

    def _make_package(name, dir=None):
        """Make a simple pip-installable package.

        Parameters
        ----------
        name : str
            Name of the package.
        dir : str, optional
            Directory in which to create the package. If not provided, the
            current directory will be used.

        Returns
        -------
        str
            Absolute path to the created package.

        Notes
        -----
        The package is declared using a ``setup.py`` to reflect today's
        ecosystem; it could be migrated to ``setup.cfg`` in the future [1]_.

        References
        ----------
        .. [1] https://setuptools.pypa.io/en/latest/userguide/quickstart.html?highlight=setup.py#transitioning-from-setup-py-to-setup-cfg

        """
        if not dir:
            dir = os.curdir

        dir = os.path.abspath(dir)
        pkg_dir = os.path.join(dir, name)
        code_dir = os.path.join(pkg_dir, name)

        os.mkdir(pkg_dir)
        created_packages.append(pkg_dir)
        with open(os.path.join(pkg_dir, "setup.py"), "w") as f:
            content = textwrap.dedent(
                """\
                # -*- coding: utf-8 -*-

                from setuptools import find_packages, setup

                setup(
                    name="{}",
                    version="0.0.1",
                    packages=find_packages(),
                )
                """.format(name)
            )
            f.write(content)
        os.mkdir(code_dir)
        with open(os.path.join(code_dir, "__init__.py"), "w") as f:
            content = textwrap.dedent(
                """\
                # -*- coding: utf-8 -*-

                is_successful = True
                """
            )
            f.write(content)

        return pkg_dir

    yield _make_package

    for pkg_dir in created_packages:
        shutil.rmtree(pkg_dir, ignore_errors=True)


@pytest.fixture
def install_local_package():
    """pip install locally-defined packages.

    Warnings
    --------
    While packages are uninstalled on teardown, their dependencies may not be.

    """
    installed_packages = []

    def _install_local_package(pkg_dir, name):
        """pip install a locally-defined package.

        Parameters
        ----------
        pkg_dir : str
            Absolute path to the package.
        name : str
            Name of the package. This is used to uninstall the package on
            teardown.

        """
        subprocess.check_call([sys.executable, "-m", "pip", "install", pkg_dir])
        installed_packages.append(name)

    yield _install_local_package

    for name in installed_packages:
        subprocess.check_call(
            [sys.executable, "-m", "pip", "uninstall", "-y", name],
        )
