# -*- coding: utf-8 -*-

import hypothesis
import hypothesis.strategies as st
import pytest

from verta._protos.public.registry import StageService_pb2
from verta.registry.stage_change import _StageChange

from .. import utils


class TestStageChange:
    @pytest.mark.parametrize(
        "stage_change_cls",
        utils.sorted_subclasses(_StageChange),
    )
    @hypothesis.given(
        comment=st.one_of(st.none(), st.text()),
        model_version_id=st.integers(min_value=0, max_value=2**64 - 1),
    )
    def test_to_proto_request(
        self,
        stage_change_cls,
        comment,
        model_version_id,
    ):
        stage_change = stage_change_cls(comment=comment)
        proto_request = stage_change._to_proto_request(model_version_id)

        assert proto_request == StageService_pb2.UpdateStageRequest(
            model_version_id=model_version_id,
            stage=stage_change_cls._STAGE,
            comment=comment,
        )

    @pytest.mark.parametrize(
        "stage_change_cls",
        utils.sorted_subclasses(_StageChange),
    )
    @hypothesis.given(
        comment=st.one_of(st.none(), st.text()),
    )
    def test_comment(self, stage_change_cls, comment):
        stage_change = stage_change_cls(comment=comment)
        assert stage_change.comment == comment
