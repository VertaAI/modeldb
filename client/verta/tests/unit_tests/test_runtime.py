# -*- coding: utf-8 -*-

"""
Tests for the runtime context logging tools
"""

from concurrent import futures
import unittest
import pytest
from typing import Any, Dict
from verta import runtime


def test_thread_safety() -> None:
    """
    Validate that the logging context being logged is thread local.
    """
    def log_in_thread(value):
        runtime.log('test_key', value)
        assert runtime._get_thread_logs() == {'test_key': value}

    with futures.ThreadPoolExecutor(max_workers=2) as executor:
        thread_1_log = {'thread_1_key': 'thread_1_val'}
        thread_2_log = {'thread_2_key': 'thread_2_val'}
        with runtime.context():
            executor.map(log_in_thread, [thread_1_log, thread_2_log])


class TestThreadLocalFunctions(unittest.TestCase):
    """ Unit tests for the functions that handle thread local variables """

    def test_init_thread_logs(self):
        """
        The logs stored in the thread local variable are blank on init.
        """
        local_dict: Dict[str, Any] = runtime._THREAD.__dict__
        assert runtime._get_thread_logs() == {}

    def test_set_thread_logs(self):
        runtime._set_thread_logs({'test_set': 'test_set'})
        assert runtime._THREAD.logs ==  {'test_set': 'test_set'}

    def test_get_thread_logs(self):
        runtime._set_thread_logs({'test_get': 'test_get'})
        assert runtime._get_thread_logs() ==  {'test_get': 'test_get'}
        runtime._set_thread_logs({})

    def test_init_thread_validate(self):
        """
        The logs stored in the thread local variable are blank on init.
        """
        assert runtime._get_validate_flag() == False

    def test_set_thread_logs(self):
        runtime._set_validate_flag(True)
        assert runtime._THREAD.validate == True

    def test_get_thread_logs(self):
        runtime._set_validate_flag(True)
        assert runtime._get_validate_flag() == True
        runtime._set_validate_flag(False)



def test_json_validation() -> None:
    """
    Validate that bad types and unserializable JSON are caught
    """
    with pytest.raises(TypeError):
        runtime._validate_json(unittest.TestCase)


@pytest.mark.parametrize(
    'key',
    [
        '',
        'not_an_ok_val_@',
        'no spaces allowed',
        '<no_brackets>',
        'no+plus_or_=equals'
    ]
)
def test_s3_validation_bad_keys(key: str):
    """
    Ensure improper keys trigger a ValueError exception
    """
    with pytest.raises(ValueError):
        runtime._validate_s3(key)


@pytest.mark.parametrize(
    'key',
    [
        'abc123xyz',
        '-/abc/(def)/!x*yz.def.hij',
        'this-key-is-just-fine!(yipEEEE*)'
        'single_quotes\'are_ok_too'
    ]
)
def test_s3_validation_good_keys(key: str):
    """
    Ensure improper keys trigger a ValueError exception
    """
    runtime._validate_s3(key)



