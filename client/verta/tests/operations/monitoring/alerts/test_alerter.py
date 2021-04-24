# -*- coding: utf-8 -*-
# pylint: disable=unidiomatic-typecheck

import hypothesis
import hypothesis.strategies as st

from verta._protos.public.monitoring import Alert_pb2 as _AlertService
from verta.operations.monitoring.alert import (
    _Alerter,
    FixedAlerter,
    ReferenceAlerter,
)
from verta.common.comparison import _VertaComparison


class TestFixed:
    @hypothesis.given(
        comparison=st.sampled_from(_VertaComparison.__subclasses__()),
        threshold=st.floats(allow_nan=False),
    )
    def test_create(self, comparison, threshold):
        alerter = FixedAlerter(comparison(threshold))

        msg = _AlertService.AlertFixed(
            threshold=threshold,
            operator=comparison._OPERATOR,
        )
        assert alerter._as_proto() == msg

    @hypothesis.given(
        comparison=st.sampled_from(_VertaComparison.__subclasses__()),
        threshold=st.floats(allow_nan=False),
    )
    def test_from_proto(self, comparison, threshold):
        msg = _AlertService.AlertFixed(
            threshold=threshold,
            operator=comparison._OPERATOR,
        )

        alerter = _Alerter._from_proto(msg)
        assert type(alerter) is FixedAlerter
        assert alerter._as_proto() == msg

    @hypothesis.given(
        comparison=st.sampled_from(_VertaComparison.__subclasses__()),
        threshold=st.floats(allow_nan=False),
    )
    def test_repr(self, comparison, threshold):
        """__repr__() does not raise exceptions"""
        alerter = FixedAlerter(comparison(threshold))

        assert repr(alerter)


class TestReference:
    @hypothesis.given(
        comparison=st.sampled_from(_VertaComparison.__subclasses__()),
        threshold=st.floats(allow_nan=False),
        reference_sample_id=st.integers(min_value=1, max_value=2 ** 64 - 1),
    )
    def test_create(self, comparison, threshold, reference_sample_id):
        alerter = ReferenceAlerter(comparison(threshold), reference_sample_id)

        msg = _AlertService.AlertReference(
            threshold=threshold,
            reference_sample_id=reference_sample_id,
            operator=comparison._OPERATOR,
        )
        assert alerter._as_proto() == msg

    @hypothesis.given(
        comparison=st.sampled_from(_VertaComparison.__subclasses__()),
        threshold=st.floats(allow_nan=False),
        reference_sample_id=st.integers(min_value=1, max_value=2 ** 64 - 1),
    )
    def test_from_proto(self, comparison, threshold, reference_sample_id):
        msg = _AlertService.AlertReference(
            threshold=threshold,
            reference_sample_id=reference_sample_id,
            operator=comparison._OPERATOR,
        )

        alerter = _Alerter._from_proto(msg)
        assert type(alerter) is ReferenceAlerter
        assert alerter._as_proto() == msg

    @hypothesis.given(
        comparison=st.sampled_from(_VertaComparison.__subclasses__()),
        threshold=st.floats(allow_nan=False),
        reference_sample_id=st.integers(min_value=1, max_value=2 ** 64 - 1),
    )
    def test_repr(self, comparison, threshold, reference_sample_id):
        """__repr__() does not raise exceptions"""
        alerter = ReferenceAlerter(comparison(threshold), reference_sample_id)

        assert repr(alerter)
