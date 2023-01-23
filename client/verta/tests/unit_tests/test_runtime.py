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
    100 threads are completed by 5 workers, which each thread verifying
    that it only holds it own thread-local value for logs.
    """
    def log_in_thread(value):
        runtime.log('test_key', value)
        assert runtime._get_thread_logs() == {'test_key': value}

    with futures.ThreadPoolExecutor(max_workers=5) as executor:
        with runtime.context():
            executor.map(log_in_thread, [{f'thread_{x}_key': f'thread_{x}_val'} for x in range(101)])


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


class TestContext(unittest.TestCase):
    """ Unit tests for the verta.runtime.context (context manager) class. """

    def test_logs_are_clean_on_entry(self):
        """ Thread local var for logs is a blank dict when entering context. """
        with runtime.context() as ctx:
            assert ctx.logs() == {}

    def test_logs_are_clean_on_exit(self):
        """ Thread local var for logs is a blank dict after exiting context. """
        with runtime.context() as ctx:
            runtime.log('fake_log', 'fake_value')
        assert runtime._get_thread_logs() == {}  # outside the context

    def test_final_log_entry_instance_variable(self):
        """ The instance of the context manager maintains the final log entry in logs(). """
        with runtime.context() as ctx:
            runtime.log('fake_log', 'fake_value')
        assert ctx.logs() == {'fake_log': 'fake_value'}  # outside the context

    def test_validate_flag_true(self):
        """ The validate argument triggers json validation as expected. """
        with pytest.raises(TypeError):
            with runtime.context(validate=True):
                runtime.log('obviously_not_jsonable', unittest.TestCase)

    def test_validate_flag_false(self):
        """ The validate argument defaults to False and does not trigger json validation. """
        with runtime.context():
            runtime.log('obviously_not_jsonable', unittest.TestCase)

    def test_validate_reset_on_exit(self):
        """ Thread local var for validate is reset to default (False) after exiting context. """
        with runtime.context(validate=True):
            runtime.log('test', {'test_key': 'test_val'})
        assert runtime._get_validate_flag() == False  # outside the context


class TestLog(unittest.TestCase):
    """ Unit tests for the verta.runtime.log() function for logging prediction context """

    def setUp(self) -> None:
        """ Some useful logging bits """
        self.log1 = dict(test_1_key=['list', 'of', 'things'])
        self.log2 = dict(test_2_key=dict(this='that'))
        self.log3 = dict(test1=self.log1, test2=self.log2)

    def test_log_function_updates_logs(self):
        """ The log function updates the log entry dictionary each time it is called. """
        with runtime.context() as ctx:
            runtime.log('test1', self.log1)
            assert ctx.logs() == dict(test1=self.log1)
            runtime.log('test2', self.log2)
            assert ctx.logs() == self.log3


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
        'no+plus_or_=equals',
        'this_string_does_not_contain_any_forbidden_characters_however_it_is_really_long_like_absurdly_too_long'
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
        '-abc-123_d34kfhx',
        'this-key-is-just-fine_yipEEEE'
    ]
)
def test_s3_validation_good_keys(key: str):
    """
    Ensure improper keys trigger a ValueError exception
    """
    runtime._validate_s3(key)



