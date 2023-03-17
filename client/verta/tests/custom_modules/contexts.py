# -*- coding: utf-8 -*-

import ast
import contextlib
import os
import shutil
import subprocess
import sys
import textwrap

from verta._internal_utils.custom_modules import CustomModules


@contextlib.contextmanager
def installable_package(name, dir=None):
    """Make a simple pip-installable package in the local filesystem.

    Parameters
    ----------
    name : str
        Name of the package.
    dir : str, optional
        Directory in which to create the package. If not provided, the
        current directory will be used.

    Yields
    ------
    str
        Absolute path to the created package.

    Warnings
    --------
    Packages should not be moved in the filesystem after they are created,
    otherwise cleanup will be unable to delete them.

    Notes
    -----
    In the spirit of general convention, the package and its root module have
    the same name. This isn't required nor universal however, and they have
    divergent character restrictions.

    The package is declared using a ``setup.py`` to reflect today's
    ecosystem; it could be migrated to ``setup.cfg`` in the future [#]_.

    References
    ----------
    .. [#] https://setuptools.pypa.io/en/latest/userguide/quickstart.html?highlight=setup.py#transitioning-from-setup-py-to-setup-cfg

    """
    if not dir:
        dir = os.curdir

    dir = os.path.abspath(dir)
    pkg_dir = os.path.join(dir, name)
    code_dir = os.path.join(pkg_dir, name)

    os.mkdir(pkg_dir)
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
            """.format(
                name,
            ),
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

    try:
        yield pkg_dir
    finally:
        shutil.rmtree(pkg_dir)


@contextlib.contextmanager
def installed_local_package(pkg_dir, name):
    """pip install a locally-defined package.

    Parameters
    ----------
    pkg_dir : str
        Absolute path to the package.
    name : str
        Name of the package. This is used to uninstall the package on
        cleanup.

    Warnings
    --------
    While packages are uninstalled on cleanup, their dependencies might not be.

    """
    subprocess.check_call(
        [
            sys.executable,
            "-m",
            "pip",
            "--no-python-version-warning",
            "install",
            "-qq",
            pkg_dir,
        ],
    )
    assert CustomModules.is_importable(name)  # verify installation

    try:
        yield
    finally:
        subprocess.check_call(
            [
                sys.executable,
                "-m",
                "pip",
                "--no-python-version-warning",
                "uninstall",
                "-y",
                name,
            ],
        )

        # delete cached module to disallow subsequent imports
        if name in sys.modules:
            del sys.modules[name]
