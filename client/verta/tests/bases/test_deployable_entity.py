# -*- coding: utf-8 -*-

import hashlib
import string
import tempfile

import hypothesis
import hypothesis.strategies as st

import six

from verta._protos.public.common import CommonService_pb2
from verta._internal_utils import _artifact_utils
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


class TestCreateArtifactMsg:
    @hypothesis.given(
        artifact_bytes=st.binary(min_size=1),
        key=st.text(
            st.characters(
                blacklist_categories=("Cs",),  # invalid UTF-8
                blacklist_characters=".",
            ),
            min_size=1,
        ),
        ext=st.text(
            st.characters(
                whitelist_categories=("Lu", "Ll", "Nd"),  # alphanumeric
            ),
            min_size=1,
        ),
        artifact_type=st.sampled_from(
            CommonService_pb2.ArtifactTypeEnum.ArtifactType.values(),
        ),
        method=st.text(min_size=1),
        framework=st.text(min_size=1),
    )
    def test_with_ext(
        self,
        artifact_bytes,
        key,
        ext,
        artifact_type,
        method,
        framework,
    ):
        with tempfile.NamedTemporaryFile(suffix="." + ext) as tempf:
            tempf.write(artifact_bytes)
            tempf.seek(0)

            artifact_msg = _DeployableEntity._create_artifact_msg(
                key,
                tempf,
                artifact_type,
                method,
                framework,
                # no explicit extension
            )

        checksum = hashlib.sha256(artifact_bytes).hexdigest()
        artifact_path = checksum + "/" + key + "." + ext
        assert artifact_msg == CommonService_pb2.Artifact(
            key=key,
            path=artifact_path,
            path_only=False,
            artifact_type=artifact_type,
            filename_extension=ext,
            serialization=method,
            artifact_subtype=framework,
        )
