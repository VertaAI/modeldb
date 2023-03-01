# -*- coding: utf-8 -*-

from types import ModuleType
from typing import Callable, List

from verta._internal_utils import model_dependencies as md


def test_list_class_functions(dependency_testing_model) -> None:
    """ Verify that all the functions in the test class are recognized and
    returned.
    """
    extracted_func_names = [f[0] for f in md.list_class_functions(dependency_testing_model)]
    assert extracted_func_names == dependency_testing_model.expected_function_names()


def test_list_modules_in_function_definition_wrapped(dependency_testing_model) -> None:
    """ Verify that modules used within a function definition are extracted
    as expected, for a function that is not wrapped in verify_io """
    func: Callable = dependency_testing_model.predict
    expected_module_names = ['verta.runtime', 'yaml']
    extracted_modules: List[str] = [
        f.__name__ for f in md.list_modules_in_function_definition(func)
    ]
    assert extracted_modules == expected_module_names


def test_list_modules_in_function_definition_unwrapped(dependency_testing_model) ->None:
    """ Verify that modules used within a function definition are extracted
    as expected, for a function that is not wrapped in verify_io """
    func: Callable = dependency_testing_model.unwrapped_predict
    expected_module_names = ['verta.runtime', 'click']
    extracted_modules: List[str] = [
        f.__name__ for f in md.list_modules_in_function_definition(func)
    ]
    assert extracted_modules == expected_module_names


def test_list_modules_in_function_signature_wrapped(dependency_testing_model) -> None:
    """ Verify that modules used in function arguments are extracted as
    expected when the function is wrapped in verify_io.
    """
    func: Callable = dependency_testing_model.predict
    expected_module_names = [
        'calendar',
        'datetime',
        'numpy',
        'google.protobuf.message',
        'pandas.core.frame',
    ]
    extracted_modules: List[str] = [
        f.__name__ for f in
        md.list_modules_in_function_signature(func)
    ]
    assert extracted_modules == expected_module_names


def test_list_modules_in_function_signature_unwrapped(dependency_testing_model) -> None:
    """ Verify that modules used in function arguments are extracted as
    expected with no function wrappers.
    """
    func: Callable = dependency_testing_model.unwrapped_predict
    expected_module_names = [
        'json.encoder',
        'collections',
        'sklearn.base',
        'cloudpickle.cloudpickle_fast',
        'requests.exceptions',
    ]
    extracted_modules: List[str] = [
        f.__name__ for f in
        md.list_modules_in_function_signature(func)
    ]
    assert extracted_modules == expected_module_names


def test_list_modules_in_function_return_type_hint_wrapped(dependency_testing_model) -> None:
    """ Verify that modules used in function return type hints are extracted
    as expected when the function is wrapped in verify_io.
    """
    func: Callable = dependency_testing_model.predict
    extracted_modules: List[str] = [
        f.__name__ for f in md.list_modules_in_function_signature(func)
    ]
    assert 'pandas.core.frame' in extracted_modules


def test_list_modules_in_function_return_type_hint_unwrapped(dependency_testing_model) -> None:
    """ Verify that modules used in function return type hints are extracted
    as expected with no function wrappers.
    """
    func: Callable = dependency_testing_model.unwrapped_predict
    extracted_modules: List[str] = [
        f.__name__ for f in md.list_modules_in_function_signature(func)
    ]
    assert 'requests.exceptions' in extracted_modules


def test_list_modules_in_function_return_type_hint_nested(dependency_testing_model) -> None:
    """ Verify that modules used in function return type hints are extracted
    as expected when nested inside another type construct.
    """
    func: Callable = dependency_testing_model.nested_type_hint
    extracted_modules: List[str] = [
        f.__name__ for f in md.list_modules_in_function_signature(func)
    ]
    assert 'torch' in extracted_modules


def test_list_modules_in_function_return_type_hint_multiple(dependency_testing_model) -> None:
    """ Verify that modules used in function return type hints are extracted
    as expected when multiple return types are specified.
    """
    func: Callable = dependency_testing_model.nested_multiple_returns_hint
    expected_module_names = ['urllib3.util.retry', 'PIL']
    extracted_modules: List[str] = [
        f.__name__ for f in md.list_modules_in_function_signature(func)
    ]
    for name in expected_module_names:
        assert name in extracted_modules


def test_function_local_annotations(dependency_testing_model) -> None:
    """ Verify that modules used in function local annotations are extracted as
    expected when the function is wrapped in verify_io.
    """
    func: Callable = dependency_testing_model.predict
    expected_modules = ['spacy.Errors']
    extracted_modules =  md.list_function_local_annotation_module_names(func)
    assert extracted_modules == expected_modules


def test_function_local_annotations_unwrapped(dependency_testing_model) -> None:
    """ Verify that modules used in function local annotations are extracted as
    expected when the function is not wrapped.
    """
    func: Callable = dependency_testing_model.unwrapped_predict
    expected_modules = ['boto3.Session']
    extracted_modules =  md.list_function_local_annotation_module_names(func)
    assert extracted_modules == expected_modules


def test_module_names_in_class(dependency_testing_model) -> None:
    """ Verify that all expected module names are extracted as expected.
    """
    extracted_module_names: List[str] =  md.module_names_in_class(dependency_testing_model)
    expected_module_names = dependency_testing_model.expected_modules()
    assert set(extracted_module_names) == set(expected_module_names)
