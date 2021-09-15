# -*- coding: utf-8 -*-

import os

import hypothesis
import hypothesis.strategies as st

from verta.environment import _Environment


class Environment(_Environment):
    """A minimal concrete class."""

    @classmethod
    def _from_proto(cls, blob_msg):
        pass

    def _as_proto(self):
        pass


class TestEnvironmentVariables:
    @hypothesis.given(
        names=st.lists(st.sampled_from(sorted(os.environ.keys()))),
    )
    def test_capture_values(self, names):
        env = Environment(
            env_vars=names,
            autocapture=False,
        )

        assert env.env_vars == {
            name: os.environ[name]
            for name in names
        }
