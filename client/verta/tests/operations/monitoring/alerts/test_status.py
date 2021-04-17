# -*- coding: utf-8 -*-

import pytest
import hypothesis
import hypothesis.strategies as st

from verta._protos.public.monitoring import Alert_pb2 as _AlertService
from verta.operations.monitoring.alert.status import (
    Alerting,
    Ok,
)


class TestAlerting:
    @hypothesis.given(sample_ids=st.lists(st.integers(), min_size=1))
    def test_creation(self, sample_ids):
        status = Alerting(sample_ids)

        assert status._ALERT_STATUS == _AlertService.AlertStatusEnum.ALERTING
        assert status._sample_ids == sample_ids

    def test_samples_required(self):
        with pytest.raises(TypeError):
            Alerting()  # pylint: disable=no-value-for-parameter

    @hypothesis.given(sample_ids=st.lists(st.integers(), min_size=1))
    def test_repr(self, sample_ids):
        """__repr__() does not raise exceptions"""
        status = Alerting([sample_ids])
        assert repr(status)


class TestOk:
    @hypothesis.given(sample_ids=st.lists(st.integers(), min_size=1))
    def test_creation(self, sample_ids):
        status = Ok(sample_ids)

        assert status._ALERT_STATUS == _AlertService.AlertStatusEnum.OK
        assert status._sample_ids == sample_ids

    def test_samples_optional(self):
        assert Ok()

    def test_repr(self):
        """__repr__() does not raise exceptions"""
        status = Ok()
        assert repr(status)
