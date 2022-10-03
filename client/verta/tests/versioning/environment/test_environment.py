# -*- coding: utf-8 -*-

import os

import hypothesis
import hypothesis.strategies as st
import pytest

from verta.environment import _Environment

from ... import strategies


class Environment(_Environment):
    """A minimal concrete class."""

    @classmethod
    def _from_proto(cls, blob_msg):
        pass

    def _as_proto(self):
        pass


class TestEnvironmentVariables:
    @pytest.mark.parametrize(
        "env_vars",
        [None, [], {}],
    )
    def test_empty(self, env_vars):
        env = Environment(
            env_vars=env_vars,
            autocapture=False,
        )

        assert env.env_vars is None

    @hypothesis.given(
        # pylint: disable=no-value-for-parameter
        env_vars=strategies.env_vars(),
    )
    def test_env_vars(self, env_vars):
        env = Environment(
            env_vars=env_vars,
            autocapture=False,
        )

        if isinstance(env_vars, list):
            env_vars = {name: os.environ[name] for name in env_vars}

        assert env.env_vars == (env_vars or None)

    @hypothesis.given(
        env_vars=st.dictionaries(
            keys=st.text(min_size=1),
            values=st.text(min_size=1),
        ),
        names=st.lists(st.sampled_from(sorted(os.environ.keys()))),
    )
    def test_add(self, env_vars, names):
        env = Environment(
            env_vars=env_vars,
            autocapture=False,
        )
        env.add_env_vars(names)

        expected_env_vars = {name: os.environ[name] for name in names}
        expected_env_vars.update(env_vars)
        assert env.env_vars == (expected_env_vars or None)

    @hypothesis.given(name=st.text(min_size=1))
    def test_not_found_error(self, name):
        hypothesis.assume(name not in os.environ)

        with pytest.raises(KeyError, match="not found in environment"):
            Environment(
                env_vars=[name],
                autocapture=False,
            )

        env = Environment(
            env_vars={"foo": "bar"},
            autocapture=False,
        )

        with pytest.raises(KeyError, match="not found in environment"):
            env.add_env_vars(
                [name],
            )
