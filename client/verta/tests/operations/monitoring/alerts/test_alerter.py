# -*- coding: utf-8 -*-

import hypothesis
import hypothesis.strategies as st

from verta._protos.public.monitoring import Alert_pb2 as _AlertService
from verta.operations.monitoring.alert import (
    FixedAlerter,
    ReferenceAlerter,
)

class TestFixed:
    @hypothesis.given(threshold=st.floats(allow_nan=False))
    def test_create(self, threshold):
        alerter = FixedAlerter(threshold)
        assert alerter._threshold == threshold

        msg = _AlertService.AlertFixed(
            threshold=threshold,
        )
        assert alerter._as_proto() == msg

    @hypothesis.given(threshold=st.floats(allow_nan=False))
    def test_repr(self, threshold):
        """__repr__() does not raise exceptions"""
        alerter = FixedAlerter(threshold)

        assert repr(alerter)


class TestReference:
    @hypothesis.given(
        threshold=st.floats(allow_nan=False),
        reference_sample_id=st.integers(min_value=1, max_value=2**64 - 1),
    )
    def test_create(self, threshold, reference_sample_id):
        alerter = ReferenceAlerter(threshold, reference_sample_id)
        assert alerter._threshold == threshold
        assert alerter._reference_sample_id == reference_sample_id

        msg = _AlertService.AlertReference(
            threshold=threshold,
            reference_sample_id=reference_sample_id,
        )
        assert alerter._as_proto() == msg

    @hypothesis.given(
        threshold=st.floats(allow_nan=False),
        reference_sample_id=st.integers(min_value=1, max_value=2**64 - 1),
    )
    def test_repr(self, threshold, reference_sample_id):
        """__repr__() does not raise exceptions"""
        alerter = ReferenceAlerter(threshold, reference_sample_id)

        assert repr(alerter)
