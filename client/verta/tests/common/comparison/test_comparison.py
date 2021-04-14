# -*- coding: utf-8 -*-

import hypothesis
import hypothesis.strategies as st

from verta._protos.public.common import CommonService_pb2 as _CommonService
from verta.common.comparison import (
    EqualTo,
    NotEqualTo,
    GreaterThan,
    GreaterThanOrEqualTo,
    LessThan,
    LessThanOrEqualTo,
)


class TestEqualTo:
    @hypothesis.given(value=st.floats(allow_nan=False))
    def test_creation(self, value):
        comparison = EqualTo(value)

        assert comparison.value == value
        assert comparison._operator_as_proto() == _CommonService.OperatorEnum.EQ

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

    @hypothesis.given(value=st.floats())
    def test_repr(self, value):
        comparison = LessThanOrEqualTo(value)

        assert comparison._SYMBOL in repr(comparison)
        assert str(comparison.value) in repr(comparison)
