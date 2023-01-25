# -*- coding: utf-8 -*-
"""
Tests for the runtime context logging tools
"""

from concurrent import futures
import unittest
import pytest
from typing import Any, Dict
from verta import runtime
import time
import random


@pytest.fixture(autouse=True)
def clean_thread():
    """
    Ensure thread-local logs are empty before and after each test.
    This is located here instead of conftest.py so the use of the
    autouse flag is limited to this module only.
    """
    runtime._set_thread_logs({})  # clean-up any existing logs.
    yield
    runtime._set_thread_logs({})  # clean-up any existing logs.


def test_logging_in_thread():
    """
    Validate that the logs being logged are thread local.
    100 threads are completed by 10 workers, which each thread verifying
    that it only holds it own thread-local value for logs.
    """
    def log_in_thread(value):
        runtime.log('test_key', value)
        time.sleep(random.uniform(0, 1))
        assert runtime._get_thread_logs() == {'test_key': value}

    with futures.ThreadPoolExecutor(max_workers=10) as executor:
        list(
            executor.map(
                log_in_thread,
                [{f'thread_{x}_key': f'thread_{x}_val'} for x in range(101)]
            )
        )


def test_thread_safe_context_manager() -> None:
    """
    Validate that multiple threads running a context manager simultaneously
    will stay thread-safe. 100 threads are completed by 10 workers, which
    each thread verifying that it only holds it own thread-local value for logs.
    """
    def log_in_context(value):
        with runtime.context() as test_ctx:
            runtime.log('test_key', value)
            time.sleep(random.uniform(0, 1))
        assert runtime._get_thread_logs() == {}
        assert test_ctx.logs() == {'test_key': value}

    with futures.ThreadPoolExecutor(max_workers=10) as executor:
        list(
            executor.map(
                log_in_context,
                [{f'thread_{x}_key': f'thread_{x}_val'} for x in range(101)]
            )
        )


def test_init_thread_logs():
    """
    The logs stored in the thread local variable are blank on init.
    """
    assert runtime._get_thread_logs() == {}


def test_set_thread_logs():
    runtime._set_thread_logs({'test_set': 'test_set'})
    assert runtime._THREAD.logs ==  {'test_set': 'test_set'}


def test_get_thread_logs():
    runtime._set_thread_logs({'test_get': 'test_get'})
    assert runtime._get_thread_logs() ==  {'test_get': 'test_get'}
    runtime._set_thread_logs({})


def test_init_thread_validate():
    """
    The logs stored in the thread local variable are blank on init.
    """
    assert runtime._get_validate_flag() == False


def test_set_thread_validate():
    runtime._set_validate_flag(True)
    assert runtime._THREAD.validate == True


def test_get_thread_validate():
    runtime._set_validate_flag(True)
    assert runtime._get_validate_flag() == True
    runtime._set_validate_flag(False)


def test_logs_are_clean_on_entry():
    """ Thread local var for logs is a blank dict when entering context. """
    with runtime.context() as ctx:
        assert ctx.logs() == {}


def test_logs_are_clean_on_exit():
    """ Thread local var for logs is a blank dict after exiting context. """
    with runtime.context() as ctx:
        runtime.log('fake_log', 'fake_value')
    assert runtime._get_thread_logs() == {}  # outside the context


def test_final_log_entry_instance_variable():
    """ The instance of the context manager maintains the final log entry in logs(). """
    with runtime.context() as ctx:
        runtime.log('fake_log', 'fake_value')
    assert ctx.logs() == {'fake_log': 'fake_value'}  # outside the context


def test_validate_flag_true():
    """ The validate argument triggers json validation as expected. """
    with runtime.context(validate=True):
        with pytest.raises(TypeError):
            runtime.log('obviously_not_jsonable', unittest.TestCase)


def test_validate_flag_false():
    """ The validate argument defaults to False and does not trigger json validation. """
    with runtime.context():
        runtime.log('obviously_not_jsonable', unittest.TestCase)


def test_validate_reset_on_exit():
    """ Thread local var for validate is reset to default (False) after exiting context. """
    with runtime.context(validate=True):
        runtime.log('test', {'test_key': 'test_val'})
        assert runtime._get_validate_flag() == True  # inside the context
    assert runtime._get_validate_flag() == False  # outside the context


def test_log_function_updates_logs():
    """ The log function updates the log entry dictionary each time it is called. """
    with runtime.context() as ctx:
        runtime.log('test1', {'test_1_key': ['list', 'of', 'things']})
        assert ctx.logs() == {'test1': {'test_1_key': ['list', 'of', 'things']}}
        runtime.log('test2', {'test_2_key': {'this': 'that'}})
        assert ctx.logs() == {
            'test1': {'test_1_key': ['list', 'of', 'things']},
            'test2': {'test_2_key': {'this': 'that'}}
        }


def test_exception_on_prior_logs() -> None:
    """
    A Runtime error is thrown if logs are added outside the context
    manager's scope that would otherwise be overwritten.
    """
    with pytest.raises(RuntimeError):
        runtime.log('outside_of_scope', {'this': 'that'})
        with runtime.context():
            pass


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
    Ensure valid keys do not throw an exception
    """
    runtime._validate_s3(key)



