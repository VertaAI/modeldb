# -*- coding: utf-8 -*-

import pytest
import hypothesis
import hypothesis.strategies as st

from verta._protos.public.monitoring import Alert_pb2 as _AlertService
from verta.monitoring.alert.status import (
    Alerting,
    Ok,
)


class TestAlerting:
    @hypothesis.given(
        sample_ids=st.lists(
            st.integers(min_value=1, max_value=2 ** 64 - 1), min_size=1,
        ),
    )
    def test_creation(self, sample_ids):
        status = Alerting(sample_ids)

        assert status._ALERT_STATUS == _AlertService.AlertStatusEnum.ALERTING
        assert status._sample_ids == sample_ids

    def test_samples_required(self):
        with pytest.raises(TypeError):
            Alerting()  # pylint: disable=no-value-for-parameter

    @hypothesis.given(
        sample_ids=st.lists(
            st.integers(min_value=1, max_value=2 ** 64 - 1), min_size=1,
        ),
    )
    def test_to_proto_request(self, sample_ids):
        status = Alerting(sample_ids)
        proto_request = status._to_proto_request()

        assert proto_request == _AlertService.UpdateAlertStatusRequest(
            status=_AlertService.AlertStatusEnum.ALERTING,
            alerting_sample_ids=sample_ids,
            ok_sample_ids=[],
            clear_alerting_sample_ids=False,
        )

    @hypothesis.given(
        sample_ids=st.lists(
            st.integers(min_value=1, max_value=2 ** 64 - 1), min_size=1,
        ),
    )
    def test_repr(self, sample_ids):
        """__repr__() does not raise exceptions"""
        status = Alerting([sample_ids])
        assert repr(status)


class TestOk:
    @hypothesis.given(
        sample_ids=st.lists(
            st.integers(min_value=1, max_value=2 ** 64 - 1), min_size=0,
        ),
    )
    def test_creation(self, sample_ids):
        status = Ok(sample_ids)

        assert status._ALERT_STATUS == _AlertService.AlertStatusEnum.OK
        assert status._sample_ids == sample_ids

    def test_samples_optional(self):
        assert Ok()

    @hypothesis.given(
        sample_ids=st.lists(
            st.integers(min_value=1, max_value=2 ** 64 - 1), min_size=1,
        ),
    )
    def test_to_proto_request(self, sample_ids):
        status = Ok(sample_ids)
        proto_request = status._to_proto_request()

        assert proto_request == _AlertService.UpdateAlertStatusRequest(
            status=_AlertService.AlertStatusEnum.OK,
            alerting_sample_ids=[],
            ok_sample_ids=sample_ids,
            clear_alerting_sample_ids=False,
        )

    def test_to_proto_request_no_sample_ids(self):
        status = Ok()
        proto_request = status._to_proto_request()

        assert proto_request == _AlertService.UpdateAlertStatusRequest(
            status=_AlertService.AlertStatusEnum.OK,
            alerting_sample_ids=[],
            ok_sample_ids=[],
            clear_alerting_sample_ids=True,
        )

    def test_repr(self):
        """__repr__() does not raise exceptions"""
        status = Ok()
        assert repr(status)
