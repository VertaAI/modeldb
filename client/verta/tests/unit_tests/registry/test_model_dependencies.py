# -*- coding: utf-8 -*-

from typing import Callable, Set

from verta._internal_utils import model_dependencies as md


def test_class_functions(dependency_testing_model) -> None:
    """ Verify that all the functions in the test class are recognized and
    returned.
    """
    expected_func_names = [
        '__init__',
        'make_dataframe',
        'make_timeout',
        'model_test',
        'nested_multiple_returns_hint',
        'nested_type_hint',
        'predict',
        'unwrapped_predict',
    ]
    extracted_func_names = [
        f.__name__ for f in md.class_functions(dependency_testing_model)
    ]
    assert set(extracted_func_names) == set(expected_func_names)


def test_modules_in_function_body_wrapped(dependency_testing_model) -> None:
    """ Verify that modules used within a function body are extracted
    as expected, for a function that is wrapped in verify_io """
    func: Callable = dependency_testing_model.predict
    expected_modules = {'verta', 'yaml'}
    extracted_modules: Set[str] = md.modules_in_function_body(func)
    assert extracted_modules == expected_modules


def test_modules_in_function_body_unwrapped(dependency_testing_model) -> None:
    """ Verify that modules used within a function body are extracted
    as expected, for a function that is not wrapped in verify_io """
    func: Callable = dependency_testing_model.unwrapped_predict
    expected_modules = {'verta', 'click'}
    extracted_modules: Set[str] = md.modules_in_function_body(func)
    assert extracted_modules == expected_modules


def test_modules_in_function_body_return_line(dependency_testing_model) -> None:
    """ Verify that modules used only within a functions return line are extracted
    as expected, including when aliased (which causes them to be stored differently)"""
    func: Callable = dependency_testing_model.make_dataframe
    expected_modules = {'pandas'}
    extracted_modules: Set[str] = md.modules_in_function_body(func)
    assert extracted_modules == expected_modules


def test_modules_in_function_signature_wrapped(dependency_testing_model) -> None:
    """ Verify that modules used in function arguments are extracted as
    expected when the function is wrapped in verify_io.
    """
    func: Callable = dependency_testing_model.predict
    expected_modules = {
        'calendar',
        'datetime',
        'numpy',
        'google',
        'pandas',
    }
    extracted_modules: Set[str] = md.modules_in_function_signature(func)
    assert extracted_modules == expected_modules


def test_modules_in_function_signature_unwrapped(dependency_testing_model) -> None:
    """ Verify that modules used in function arguments are extracted as
    expected with no function wrappers.
    """
    func: Callable = dependency_testing_model.unwrapped_predict
    expected_modules = {
        'json',
        'collections',
        'sklearn',
        'cloudpickle',
        'requests',
    }
    extracted_modules: Set[str] = md.modules_in_function_signature(func)
    assert extracted_modules == expected_modules


def test_modules_in_function_return_type_hint_nested(dependency_testing_model) -> None:
    """ Verify that modules used in function return type hints are extracted
    as expected when nested inside another type construct.
    """
    func: Callable = dependency_testing_model.nested_type_hint
    expected_modules = {'torch'}
    extracted_modules: Set[str] = md.modules_in_function_signature(func)
    assert extracted_modules == expected_modules


def test_modules_in_function_return_type_hint_multiple(dependency_testing_model) -> None:
    """ Verify that modules used in function return type hints are extracted
    as expected when multiple return types are specified.
    """
    func: Callable = dependency_testing_model.nested_multiple_returns_hint
    expected_modules = {'urllib3', 'PIL'}
    extracted_modules: Set[str] = md.modules_in_function_signature(func)
    assert extracted_modules == expected_modules


def test_class_module_names(dependency_testing_model) -> None:
    """ Verify that all expected module names are extracted as expected.
    """
    expected_modules = {
        'calendar',
        'click',
        'cloudpickle',
        'collections',
        'datetime',
        'google',
        'json',
        'numpy',
        'pandas',
        'PIL',
        'requests',
        'requests',
        'sklearn',
        'torch',
        'urllib3',
        'verta',
        'yaml',
    }
    extracted_modules: Set[str] =  md.class_module_names(dependency_testing_model)
    assert set(extracted_modules) == set(expected_modules)
