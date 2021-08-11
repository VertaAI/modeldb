# -*- coding: utf-8 -*-

# for composite strategy
# pylint: disable=no-value-for-parameter

import hypothesis
import hypothesis.strategies as st

from verta._protos.public.common import CommonService_pb2 as _CommonService
from verta.monitoring.comparison import (
    _VertaComparison,
    EqualTo,
    NotEqualTo,
    GreaterThan,
    GreaterThanOrEqualTo,
    LessThan,
    LessThanOrEqualTo,
)


@st.composite
def lower_and_higher(draw):
    lower = draw(st.floats(allow_nan=False, allow_infinity=False))
    higher = draw(st.floats(min_value=lower, exclude_min=True, allow_nan=False))
    hypothesis.assume(not _VertaComparison.isclose(lower, higher))

    return lower, higher


class TestEqualTo:
    @hypothesis.given(value=st.floats(allow_nan=False))
    def test_creation(self, value):
        comparison = EqualTo(value)

        assert comparison.value == value
        assert comparison._operator_as_proto() == _CommonService.OperatorEnum.EQ

    @hypothesis.given(lower_and_higher=lower_and_higher())
    def test_compare(self, lower_and_higher):
        lower, higher = lower_and_higher

        assert EqualTo(lower).compare(lower)
        assert not EqualTo(lower).compare(higher)

    @hypothesis.given(value=st.floats())
    def test_repr(self, value):
        comparison = EqualTo(value)

        assert comparison._SYMBOL in repr(comparison)
        assert str(comparison.value) in repr(comparison)


class TestNotEqualTo:
    @hypothesis.given(value=st.floats(allow_nan=False))
    def test_creation(self, value):
        comparison = NotEqualTo(value)

        assert comparison.value == value
        assert comparison._operator_as_proto() == _CommonService.OperatorEnum.NE

    @hypothesis.given(lower_and_higher=lower_and_higher())
    def test_compare(self, lower_and_higher):
        lower, higher = lower_and_higher

        assert NotEqualTo(lower).compare(higher)
        assert not NotEqualTo(lower).compare(lower)

    @hypothesis.given(value=st.floats())
    def test_repr(self, value):
        comparison = NotEqualTo(value)

        assert comparison._SYMBOL in repr(comparison)
        assert str(comparison.value) in repr(comparison)


class TestGreaterThan:
    @hypothesis.given(value=st.floats(allow_nan=False))
    def test_creation(self, value):
        comparison = GreaterThan(value)

        assert comparison.value == value
        assert comparison._operator_as_proto() == _CommonService.OperatorEnum.GT

    @hypothesis.given(lower_and_higher=lower_and_higher())
    def test_compare(self, lower_and_higher):
        lower, higher = lower_and_higher

        assert GreaterThan(lower).compare(higher)
        assert not GreaterThan(lower).compare(lower)
        assert not GreaterThan(higher).compare(lower)

    @hypothesis.given(value=st.floats())
    def test_repr(self, value):
        comparison = GreaterThan(value)

        assert comparison._SYMBOL in repr(comparison)
        assert str(comparison.value) in repr(comparison)


class TestGreaterThanOrEqualTo:
    @hypothesis.given(value=st.floats(allow_nan=False))
    def test_creation(self, value):
        comparison = GreaterThanOrEqualTo(value)

        assert comparison.value == value
        assert comparison._operator_as_proto() == _CommonService.OperatorEnum.GTE

    @hypothesis.given(lower_and_higher=lower_and_higher())
    def test_compare(self, lower_and_higher):
        lower, higher = lower_and_higher

        assert GreaterThanOrEqualTo(lower).compare(lower)
        assert GreaterThanOrEqualTo(lower).compare(higher)
        assert not GreaterThanOrEqualTo(higher).compare(lower)

    @hypothesis.given(value=st.floats())
    def test_repr(self, value):
        comparison = GreaterThanOrEqualTo(value)

        assert comparison._SYMBOL in repr(comparison)
        assert str(comparison.value) in repr(comparison)


class TestLessThan:
    @hypothesis.given(value=st.floats(allow_nan=False))
    def test_creation(self, value):
        comparison = LessThan(value)

        assert comparison.value == value
        assert comparison._operator_as_proto() == _CommonService.OperatorEnum.LT

    @hypothesis.given(lower_and_higher=lower_and_higher())
    def test_compare(self, lower_and_higher):
        lower, higher = lower_and_higher

        assert LessThan(higher).compare(lower)
        assert not LessThan(higher).compare(higher)
        assert not LessThan(lower).compare(higher)

    @hypothesis.given(value=st.floats())
    def test_repr(self, value):
        comparison = LessThan(value)

        assert comparison._SYMBOL in repr(comparison)
        assert str(comparison.value) in repr(comparison)


class TestLessThanOrEqualTo:
    @hypothesis.given(value=st.floats(allow_nan=False))
    def test_creation(self, value):
        comparison = LessThanOrEqualTo(value)

        assert comparison.value == value
        assert comparison._operator_as_proto() == _CommonService.OperatorEnum.LTE

    @hypothesis.given(lower_and_higher=lower_and_higher())
    def test_compare(self, lower_and_higher):
        lower, higher = lower_and_higher

        assert LessThanOrEqualTo(higher).compare(higher)
        assert LessThanOrEqualTo(higher).compare(lower)
        assert not LessThanOrEqualTo(lower).compare(higher)

    @hypothesis.given(value=st.floats())
    def test_repr(self, value):
        comparison = LessThanOrEqualTo(value)

        assert comparison._SYMBOL in repr(comparison)
        assert str(comparison.value) in repr(comparison)
