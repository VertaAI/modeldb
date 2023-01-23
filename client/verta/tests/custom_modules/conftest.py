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
