# -*- coding: utf-8 -*-

from verta.operations.monitoring.alert.status import (
    Alerting,
    Ok,
)


class TestAlerting:
    def test_repr(self):
        """__repr__() does not raise exceptions"""
        status = Alerting()
        assert repr(status)


class TestOk:
    def test_repr(self):
        """__repr__() does not raise exceptions"""
        status = Ok()
        assert repr(status)
