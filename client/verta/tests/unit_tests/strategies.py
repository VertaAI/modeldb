# -*- coding: utf-8 -*-

"""Hypothesis composite strategies for use in client unit tests."""

from string import ascii_letters, ascii_lowercase, hexdigits
from typing import List

import hypothesis.strategies as st

from verta._internal_utils._utils import _VALID_FLAT_KEY_CHARS
from verta._protos.public.common import CommonService_pb2
from verta._protos.public.modeldb.versioning import Code_pb2, Dataset_pb2


@st.composite
def uint64(draw) -> int:
    """Generate an integer within the range of an uint64."""
    return draw(st.integers(min_value=0, max_value=2**64 - 1))


@st.composite
def int64(draw) -> int:
    """Generate an integer within the range of an int64."""
    return draw(st.integers(min_value=-2**63, max_value=2**63 - 1))


@st.composite
def artifact_proto(draw) -> CommonService_pb2.Artifact:
    """Generate a mocked Artifact protobuf object."""
    key: str = draw(st.text(_VALID_FLAT_KEY_CHARS, min_size=1))
    filename_extension: str = draw(st.text(ascii_lowercase))

    return CommonService_pb2.Artifact(
        key=key,
        path= f"{draw(st.text(ascii_letters + '/'))}/{key}.{filename_extension}",
        filename_extension=filename_extension,

        artifact_type=CommonService_pb2.ArtifactTypeEnum.BLOB,
        upload_completed=draw(st.booleans()),

        # these possible values are based on _artifact_utils.serialize_model()
        serialization=draw(st.sampled_from(["joblib", "cloudpickle", "pickle", "keras"])),
    )


@st.composite
def model_artifact_proto(draw) -> CommonService_pb2.Artifact:
    """
    Generate a mocked Artifact protobuf object specifically representing a model artifact.

    """
    proto: CommonService_pb2.Artifact = draw(artifact_proto())
    proto.artifact_type = CommonService_pb2.ArtifactTypeEnum.MODEL
    proto.artifact_subtype = draw(
        # these possible values are based on _artifact_utils.serialize_model()
        st.sampled_from(["torch", "sklearn", "xgboost", "tensorflow", "custom", "callable"]),
    )

    return proto


@st.composite
def git_code_blob_proto(draw) -> Code_pb2.GitCodeBlob:
    """Generate a mocked GitCodeBlob protobuf object."""
    return Code_pb2.GitCodeBlob(
        repo=draw(st.text(ascii_letters)),
        hash=draw(st.text(hexdigits)),
        branch=draw(st.text(ascii_letters)),
        tag=draw(st.text(ascii_letters)),
        is_dirty=draw(st.booleans()),
    )


@st.composite
def notebook_code_blob_proto(draw) -> Code_pb2.NotebookCodeBlob:
    """Generate a mocked NotebookCodeBlob protobuf object."""
    return Code_pb2.NotebookCodeBlob(
        path=Dataset_pb2.PathDatasetComponentBlob(
            path=draw(st.text(ascii_letters)),
            size=draw(uint64()),
            last_modified_at_source=draw(uint64()),
            md5=draw(st.text(hexdigits)),
        ),
        git_repo=draw(git_code_blob_proto()),
    )


@st.composite
def code_blob_proto(draw) -> Code_pb2.CodeBlob:
    """Generate a mocked CodeBlob protobuf object."""
    proto = Code_pb2.CodeBlob()
    if draw(st.booleans()):
        proto.git.CopyFrom(draw(git_code_blob_proto()))
    else:
        proto.notebook.CopyFrom(draw(notebook_code_blob_proto()))

    return proto
