# -*- coding: utf-8 -*-
# pylint: disable=unidiomatic-typecheck

import hypothesis
import hypothesis.strategies as st

from verta._protos.public.monitoring import Alert_pb2 as _AlertService
from verta.monitoring.alert import (
    _Alerter,
    FixedAlerter,
    RangeAlerter,
    ReferenceAlerter,
)
from verta.monitoring.comparison import _VertaComparison


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
        assert alerter.comparison is not None

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
        assert alerter.comparison is not None
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


class TestRange:
    @hypothesis.given(
        lower_bound=st.floats(allow_nan=False),
        upper_bound=st.floats(allow_nan=False),
        alert_if_outside_range=st.booleans(),
    )
    def test_to_proto(self, lower_bound, upper_bound, alert_if_outside_range):
        alerter = RangeAlerter(
            lower_bound=lower_bound,
            upper_bound=upper_bound,
            alert_if_outside_range=alert_if_outside_range,
        )
        msg = _AlertService.AlertRange(
            lower_bound=lower_bound,
            upper_bound=upper_bound,
            alert_if_outside_range=alert_if_outside_range,
        )
        assert alerter._as_proto() == msg

    @hypothesis.given(
        lower_bound=st.floats(allow_nan=False),
        upper_bound=st.floats(allow_nan=False),
        alert_if_outside_range=st.booleans(),
    )
    def test_from_proto(self, lower_bound, upper_bound, alert_if_outside_range):
        msg = _AlertService.AlertRange(
            lower_bound=lower_bound,
            upper_bound=upper_bound,
            alert_if_outside_range=alert_if_outside_range,
        )
        alerter = _Alerter._from_proto(msg)
        assert type(alerter) is RangeAlerter
        assert alerter._as_proto() == msg
