# -*- coding: utf-8 -*-

import ast
import inspect
import textwrap
from types import ModuleType
from typing import Callable, get_type_hints, List, Tuple, Type

from verta.registry import VertaModelBase


def list_class_functions(model_class: Type[VertaModelBase]) -> List[Tuple[str, Callable]]:
    """List all the functions present in the provided class object."""
    return inspect.getmembers(model_class, predicate=inspect.isfunction)


def unwrap(func: Callable) -> Callable:
    """Unwraps a function to get the original function object in a non-blocking manner."""
    try:
        return inspect.unwrap(func)  # returns last object in chain
    except ValueError:  # if a cycle is encountered
        return func


def list_modules_in_function_body(func: Callable) -> List[str]:
    """List all base modules called within the body of the provided function."""
    _func = unwrap(func)
    globals = [
        value.__name__.split('.')[0]  # strip off submodules and classes
        for key, value in inspect.getclosurevars(_func).globals.items()
        if isinstance(value, ModuleType)
    ]
    non_locals = [
       value.__name__.split('.')[0]  # strip off submodules and classes
       for key, value in inspect.getclosurevars(_func).nonlocals.items()
       if isinstance(value, ModuleType)
    ]
    return globals + non_locals


def list_modules_in_function_signature(func: Callable) -> List[str]:
    """List all base modules used in type hints in the provided function's arguments
    and return type hint."""
    _func = unwrap(func)
    hints = get_type_hints(_func)
    arg_hints = { k: v for k, v in hints.items() if k != 'return' }
    return_hint = hints.get('return')

    modules = [ inspect.getmodule(value) for key, value in arg_hints.items() ]

    if return_hint:
        mod = inspect.getmodule(return_hint)
        if mod.__name__ == 'typing':
            ret_ann = inspect.signature(_func).return_annotation
            nested_args = ret_ann.__args__
            for a in nested_args:
                if inspect.isclass(a):
                    modules.append(inspect.getmodule(a))
        else:
            modules.append(inspect.getmodule(return_hint))
    return  [ m.__name__.split('.')[0] for m in modules ]


def list_class_module_names(model_class: Type[VertaModelBase]) -> List[str]:
    """Attempt to list all base modules used in the provided class object."""
    funcs = list_class_functions(model_class)
    modules_found = list()
    for function in funcs:
        _func = function[1]
        mods_in_body = list_modules_in_function_body(_func)
        mods_in_signature = list_modules_in_function_signature(_func)
        modules_found += mods_in_body + mods_in_signature
    return list(set(modules_found))