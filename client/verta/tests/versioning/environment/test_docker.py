# -*- coding: utf-8 -*-

import hypothesis
import hypothesis.strategies as st
import pytest
import six

from verta._protos.public.modeldb.versioning import (
    Environment_pb2,
    VersioningService_pb2,
)
from verta.environment import Docker


class TestObject:
    @hypothesis.given(
        repository=st.text(min_size=1),
        tag=st.one_of(st.none(), st.text(min_size=1)),
        sha=st.one_of(st.none(), st.text(min_size=1)),
    )
    def test_repr(self, repository, tag, sha):
        hypothesis.assume(tag or sha)

        env = Docker(repository=repository, tag=tag, sha=sha)

        assert "repository: {}".format(six.ensure_str(repository)) in repr(env)
        if tag:
            assert "tag: {}".format(six.ensure_str(tag)) in repr(env)
        else:
            assert "tag: " not in repr(env)
        if sha:
            assert "sha: {}".format(six.ensure_str(sha)) in repr(env)
        else:
            assert "sha: " not in repr(env)

    def test_required_arguments(self):
        err_msg_match = (
            r"takes at least 2 arguments \(1 given\)"
            if six.PY2
            else "missing 1 required positional argument: 'repository'"
        )
        with pytest.raises(
            TypeError,
            match=err_msg_match,
        ):
            Docker()  # pylint: disable=no-value-for-parameter

        with pytest.raises(
            ValueError,
            match="must at least specify either `tag` or `sha`",
        ):
            Docker(repository="foo")

    @hypothesis.given(
        repository=st.text(min_size=1),
        tag=st.one_of(st.none(), st.text(min_size=1)),
        sha=st.one_of(st.none(), st.text(min_size=1)),
    )
    def test_properties(self, repository, tag, sha):
        hypothesis.assume(tag or sha)

        env = Docker(repository=repository, tag=tag, sha=sha)

        assert env.repository == repository
        assert env.tag == tag or None
        assert env.sha == sha or None


class TestProto:
    @hypothesis.given(
        repository=st.text(min_size=1),
        tag=st.one_of(st.none(), st.text(min_size=1)),
        sha=st.one_of(st.none(), st.text(min_size=1)),
    )
    def test_from_proto(self, repository, tag, sha):
        hypothesis.assume(tag or sha)

        msg = VersioningService_pb2.Blob(
            environment=Environment_pb2.EnvironmentBlob(
                docker=Environment_pb2.DockerEnvironmentBlob(
                    repository=repository,
                    tag=tag,
                    sha=sha,
                )
            )
        )
        env = Docker._from_proto(msg)

        assert env.repository == repository
        assert env.tag == tag or None
        assert env.sha == sha or None

    @hypothesis.given(
        repository=st.text(min_size=1),
        tag=st.one_of(st.none(), st.text(min_size=1)),
        sha=st.one_of(st.none(), st.text(min_size=1)),
    )
    def test_to_proto(self, repository, tag, sha):
        hypothesis.assume(tag or sha)

        env = Docker(repository=repository, tag=tag, sha=sha)
        expected_msg = VersioningService_pb2.Blob(
            environment=Environment_pb2.EnvironmentBlob(
                docker=Environment_pb2.DockerEnvironmentBlob(
                    repository=repository,
                    tag=tag,
                    sha=sha,
                )
            )
        )

        assert env._as_proto() == expected_msg
