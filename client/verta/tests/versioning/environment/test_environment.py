# -*- coding: utf-8 -*-

import os

import hypothesis
import hypothesis.strategies as st
import pytest

from verta.environment import _Environment


class Environment(_Environment):
    """A minimal concrete class."""

    @classmethod
    def _from_proto(cls, blob_msg):
        pass

    def _as_proto(self):
        pass


class TestEnvironmentVariables:
    @pytest.mark.parametrize(
        "env_vars", [None, [], {}],
    )
    def test_empty(self, env_vars):
        env = Environment(
            env_vars=env_vars,
            autocapture=False,
        )

        assert env.env_vars is None

    @hypothesis.given(
        env_vars=st.dictionaries(
            keys=st.text(min_size=1),
            values=st.text(min_size=1),
        ),
    )
    def test_provided_values(self, env_vars):
        env = Environment(
            env_vars=env_vars,
            autocapture=False,
        )

        assert env.env_vars == env_vars

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

        expected_env_vars = {
            name: os.environ[name]
            for name in names
        }
        expected_env_vars.update(env_vars)
        assert env.env_vars == expected_env_vars

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
