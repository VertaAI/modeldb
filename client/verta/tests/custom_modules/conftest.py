# -*- coding: utf-8 -*-

import os
import shutil
import tempfile

import pytest

from .. import constants
from .. import utils


@pytest.fixture(scope="class")
def in_fake_venv():
    """Move test to execute inside a mocked, empty virtual environment."""
    dirpath = tempfile.mkdtemp(dir=utils.TEMPDIR_ROOT)

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


# TODO: move to root conftest and substitute for `mkdtemp`s in tests
@pytest.fixture(scope="class")
def make_tempdir():
    """Make temporary directories.

    Analogous to ``pytest``'s built-in ``tmp_path_factory`` fixture, except
    this doesn't require explicit names for its directories.

    """
    created_dirs = []

    def _make_tempdir():
        """Make a temporary directory.

        Returns
        -------
        str
            Absolute path to the created directory.

        """
        dirpath = tempfile.mkdtemp(dir=utils.TEMPDIR_ROOT)
        created_dirs.append(dirpath)
        return dirpath

    yield _make_tempdir

    for dirpath in created_dirs:
        shutil.rmtree(dirpath)
