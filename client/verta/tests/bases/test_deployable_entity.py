# -*- coding: utf-8 -*-

import hashlib

import hypothesis
import hypothesis.strategies as st

import six

from verta.tracking.entities._deployable_entity import _DeployableEntity


class TestBuildArtifactStorePath:
    @hypothesis.example(
        artifact_bytes=b"foo",
        key="my_artifact",
        ext="pkl",
    )
    @hypothesis.given(
        artifact_bytes=st.binary(min_size=1),
        key=st.text(st.characters(blacklist_characters="."), min_size=1),
        ext=st.text(st.characters(blacklist_characters="."), min_size=1),
    )
    def test_with_ext(self, artifact_bytes, key, ext):
        checksum = hashlib.sha256(artifact_bytes).hexdigest()
        filename = key + "." + ext
        expected_path = checksum + "/" + filename

        artifact_path = _DeployableEntity._build_artifact_store_path(
            artifact_stream=six.BytesIO(artifact_bytes),
            key=key,
            ext=ext,
        )

        assert artifact_path == expected_path

    @hypothesis.example(artifact_bytes=b"foo", key="model")
    @hypothesis.example(artifact_bytes=b"foo", key="model_api.json")
    @hypothesis.given(
        artifact_bytes=st.binary(min_size=1),
        key=st.text(min_size=1),
    )
    def test_no_ext(self, artifact_bytes, key):
        checksum = hashlib.sha256(artifact_bytes).hexdigest()
        filename = key
        expected_path = checksum + "/" + filename

        artifact_path = _DeployableEntity._build_artifact_store_path(
            artifact_stream=six.BytesIO(artifact_bytes),
            key=key,
        )

        assert artifact_path == expected_path
