# -*- coding: utf-8 -*-

from types import ModuleType
from typing import Callable, List
from verta._internal_utils import model_dependencies as md


def test_list_class_functions(dependency_testing_model) -> None:
    """ Verify that all the functions in the test class are recognized and
    returned.
    """
    expected_function_names = [
        '__init__',
        'predict',
        'unwrapped_predict',
        'make_dataframe',
        'make_timeout',
        'make_spacy_error',
        'make_boto_session',
        'nested_multiple_returns_hint',
        'nested_type_hint'
    ]
    extracted_func_names = [f[0] for f in md.list_class_functions(dependency_testing_model)]
    assert extracted_func_names.sort() == expected_function_names.sort()


def test_is_wrapped(dependency_testing_model) -> None:
    """ Verify that the is_wrapped function correctly identifies wrapped functions """
    assert md.is_wrapped(dependency_testing_model.predict)
    assert not md.is_wrapped(dependency_testing_model.unwrapped_predict)


def test_list_modules_in_function_definition_unwrapped(dependency_testing_model) ->None:
    """ Verify that modules used within a function definition are extracted
    as expected, for a function that is not wrapped in verify_io """
    func: Callable = dependency_testing_model.unwrapped_predict
    expected_module_names = ['click', 'verta']  # TODO pick up boto3
    extracted_modules: List[ModuleType] = [
        f.__package__ for f in
        md.list_modules_in_function_definition(func)
    ]
    assert extracted_modules.sort() == expected_module_names.sort()


def test_list_modules_in_function_definition_wrapped(dependency_testing_model) -> None:
    """ Verify that modules used within a function definition are extracted
    as expected, for a function that is not wrapped in verify_io """
    func: Callable = dependency_testing_model.predict
    expected_module_names = ['yaml', 'spacy']
    extracted_modules: List[ModuleType] = [
        f.__package__ for f in
        md.list_modules_in_function_definition(func)
    ]
    assert extracted_modules == expected_module_names
