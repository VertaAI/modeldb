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
    Ensure thread-local logs are wiped before and after each test.
    This is located here instead of conftest.py so the use of the
    autouse flag is limited to this module only.
    """
    yield
    if hasattr(runtime._THREAD, 'logs'):
        delattr(runtime._THREAD, 'logs')  # clean-up any existing logs.
    if hasattr(runtime._THREAD, 'validate'):
        delattr(runtime._THREAD, 'validate')  # clean-up any existing flag.



def test_thread_safe_context_manager() -> None:
    """
    Validate that multiple threads running a context manager simultaneously
    will stay thread-safe. 50 threads are completed by 10 workers, with
    each thread verifying that it only holds it own thread-local value for logs.
    """
    def log_in_context(value):
        with runtime.context() as test_ctx:
            runtime.log('test_key', value)
        assert hasattr(runtime._THREAD, 'logs') == False
        assert hasattr(runtime._THREAD, 'validate') == False
        assert test_ctx.logs() == {'test_key': value}

    with futures.ThreadPoolExecutor(max_workers=10) as executor:
        list(
            executor.map(
                log_in_context,
                [{f'thread_{x}_key': f'thread_{x}_val'} for x in range(50)]
            )
        )


def test_set_thread_logs():
    runtime._set_thread_logs({'test_set': 'test_set'})
    assert runtime._THREAD.logs ==  {'test_set': 'test_set'}


def test_get_thread_logs():
    runtime._set_thread_logs({'test_get': 'test_get'})
    assert runtime._get_thread_logs() ==  {'test_get': 'test_get'}


def test_set_thread_validate():
    runtime._set_validate_flag(True)
    assert runtime._THREAD.validate == True


def test_get_thread_validate():
    runtime._set_validate_flag(True)
    assert runtime._get_validate_flag() == True


def test_logs_are_clean_on_entry():
    """ Thread local var for logs is a blank dict when entering context. """
    with runtime.context() as ctx:
        assert ctx.logs() == {}


def test_logs_attribute_removed_on_exit():
    """ Thread local var for logs is removed after exiting the context. """
    with runtime.context() as ctx:
        runtime.log('fake_log', 'fake_value')
        assert ctx.logs()
    assert not hasattr(runtime._THREAD, 'logs')  # outside the context


def test_final_log_entry_instance_variable():
    """ The instance of the context manager maintains the final log entry in the
    logs_dict instance attribute. """
    with runtime.context() as ctx:
        runtime.log('fake_log', 'fake_value')
    assert ctx.logs() == {'fake_log': 'fake_value'}  # outside the context


def test_empty_dict_returned_when_no_logs_added():
    """ If no calls are made to runtime.log() within the context, calling for
    context.logs() returns an empty dict."""
    with runtime.context() as ctx:
        pass
    assert ctx.logs() == {}


def test_validate_flag_true():
    """ The validate argument triggers json validation as expected. """
    with runtime.context(validate=True):
        with pytest.raises(TypeError):
            runtime.log('obviously_not_jsonable', unittest.TestCase)


def test_validate_flag_false():
    """ The validate argument defaults to False and does not trigger json validation. """
    with runtime.context():
        runtime.log('obviously_not_jsonable', unittest.TestCase)


def test_validate_variable_removed_on_exit():
    """ Thread local attribute for validate is removed after exiting the context. """
    with runtime.context(validate=True):
        runtime.log('test', {'test_key': 'test_val'})
        assert runtime._get_validate_flag() == True  # inside the context
    assert hasattr(runtime._THREAD, 'validate') == False  # outside the context


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


def test_exception_on_nesting_context_managers() -> None:
    """
    Attempting to open a new
    """
    with pytest.raises(RuntimeError) as err:
        with runtime.context():
            with runtime.context():
                pass
    assert err.value.args[0] == "nesting an instance of verta.runtime.context() inside" \
                                " an existing instance is not supported."


def test_exception_on_logging_outside_any_context() -> None:
    """
    Calling runtime.log() while outside of any instance of
    runtime.context() will raise a RuntimeError exception.
    """
    with pytest.raises(RuntimeError) as err:
        runtime.log('outside_of_context_manager', {'this': 'that'})
        assert err.value.args[0] == " calls to verta.runtime.log() must be made" \
                                    " within the scope of a model's predict() method."



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


def test_overwrite_existing_key_throws_error() -> None:
    """
    If an attempt is made to write to the same key twice within a single
    context, a KeyError exception is raised.
    """
    with runtime.context() as ctx:
        runtime.log('key_123', 'value_1')
        with pytest.raises(ValueError)as err:
            runtime.log('key_123', 'value_2')
        assert err.value.args[0] == " cannot overwrite existing value for \"key_123\""
        assert ctx.logs() == {'key_123': 'value_1'}
