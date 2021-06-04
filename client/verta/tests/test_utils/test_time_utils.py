from datetime import datetime, timedelta

from hypothesis import given
import hypothesis.strategies as st
import pytest
from verta._internal_utils import time_utils
from ..time_strategies import millis_uint64_strategy, millis_timedelta_strategy


def _check_positive_millis(millis):
    assert isinstance(millis, int) and millis > 0


class TestEpochMillis:
    def test_none(self):
        assert time_utils.epoch_millis(None) is None

    def test_naive_dt(self):
        naive_dt = datetime.now()
        assert naive_dt.tzinfo is None
        with pytest.warns(UserWarning):
            millis = time_utils.epoch_millis(naive_dt)
        _check_positive_millis(millis)

    def test_aware_dt(self):
        aware_dt = datetime.now(time_utils.utc)
        assert aware_dt.tzinfo is not None
        _check_positive_millis(time_utils.epoch_millis(aware_dt))

    def test_pass_through_positive_int(self):
        epoch_millis = 5
        _check_positive_millis(time_utils.epoch_millis(epoch_millis))


class TestDurationMillis:
    def test_zero_millis(self):
        assert time_utils.duration_millis(0) == 0
        assert time_utils.duration_millis(timedelta()) == 0

    @given(millis=millis_uint64_strategy)
    def test_duration_millis(self, millis):
        duration = time_utils.duration_millis(millis)
        _check_positive_millis(duration)

    @given(millis=millis_uint64_strategy)
    def test_duration_millis_delta(self, millis):
        delta = timedelta(milliseconds=millis)
        duration = time_utils.duration_millis(delta)
        _check_positive_millis(duration)

    @given(millis=millis_uint64_strategy)
    def test_downsample_resolution(self, millis):
        delta = timedelta(milliseconds=millis, microseconds=2)
        with pytest.warns(UserWarning):
            duration = time_utils.duration_millis(delta)
        assert duration == millis

    @given(negative_int=st.integers(max_value=-1))
    def test_reject_negative_duration(self, negative_int):
        with pytest.raises(ValueError):
            time_utils.duration_millis(negative_int)

    @given(millis=millis_uint64_strategy)
    def test_round_trip_from_delta(self, millis):
        delta = timedelta(milliseconds=millis)
        delta_millis = time_utils.duration_millis(delta)
        delta_parsed = time_utils.parse_duration(delta_millis)
        assert delta == delta_parsed


class TestParseDuration:
    def test_parse_duration(self):
        one_day = time_utils.parse_duration("1d")
        assert one_day == timedelta(days=1)

    def test_parse_zero(self):
        zero_dur = time_utils.parse_duration(0)
        assert isinstance(zero_dur, timedelta)

    @given(millis=millis_uint64_strategy)
    def test_parse_duration_delta(self, millis):
        delta = timedelta(milliseconds=millis)
        duration = time_utils.parse_duration(delta)
        assert duration == delta

    @given(millis=millis_uint64_strategy)
    def test_downsample_resolution(self, millis):
        delta = timedelta(milliseconds=millis, microseconds=5)
        with pytest.warns(UserWarning):
            duration = time_utils.parse_duration(delta)
        assert duration == timedelta(milliseconds=millis)
