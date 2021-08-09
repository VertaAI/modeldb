# -*- coding: utf-8 -*-

import os

import hypothesis
import hypothesis.strategies as st

from verta._internal_utils import _utils

from .. import constants
from .. import strategies


class TestVenv:
    @hypothesis.given(
        prefix=st.sampled_from(
            [
                constants.LIB_SITE_PACKAGES,
                constants.LIB32_SITE_PACKAGES,
                constants.LIB64_SITE_PACKAGES,
                constants.BIN_PYCACHE,
            ],
        ),
        filepath=strategies.filepath(),  # pylint: disable=no-value-for-parameter
    )
    def test_is_in_venv(self, prefix, filepath, in_fake_venv):
        abs_filepath = os.path.abspath(os.path.join(prefix, filepath))
        assert _utils.is_in_venv(abs_filepath)

    def test_is_not_in_venv(self):
        # NOTE: assumes there isn't a venv in the client test dir
        for root, _, files in os.walk("."):
            for filename in files:
                abs_filepath = os.path.abspath(os.path.join(root, filename))
                assert not _utils.is_in_venv(abs_filepath)
