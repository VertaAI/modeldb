# -*- coding: utf-8 -*-

import inspect
from typing import Callable, List, Tuple, Type, get_type_hints
from types import ModuleType
from verta.registry import VertaModelBase


def list_class_functions(model_class: Type[VertaModelBase]) -> List[Tuple[str, Callable]]:
    """List all the functions present in the provided class object."""
    return inspect.getmembers(model_class, predicate=inspect.isfunction)


def list_modules_in_function_definition(func: Callable) -> List[ModuleType]:
    """List all modules called within the body of the provided function."""
    _func = unwrap(func)
    return [
        value for key, value in inspect.getclosurevars(_func).globals.items()
        if isinstance(value, ModuleType)
    ]


def list_modules_in_function_signature(func: Callable) -> List[ModuleType]:
    """List all modules used in type hints in the provided function's arguments
    and return type hint."""
    _func = unwrap(func)
    hints = get_type_hints(_func)
    arg_hints = { k: v for k, v in hints.items() if k != 'return' }
    return_hint = { k: v for k, v in hints.items() if k == 'return' }

    modules = [ inspect.getmodule(value) for key, value in arg_hints.items() ]

    if return_hint:
        mod = inspect.getmodule(return_hint['return'])
        if mod.__name__ == 'typing':
            ret_ann = inspect.signature(_func).return_annotation
            nested_args = ret_ann.__args__
            for a in nested_args:
                if inspect.isclass(a):
                    modules.append(inspect.getmodule(a))
        else:
            modules.append(inspect.getmodule(return_hint['return']))
    return modules


def unwrap(func: Callable) -> Callable:
    """Unwraps a function to get the original function object in a non-blocking manner."""
    try:
        return inspect.unwrap(func)  # returns last object in chain
    except ValueError:  # if a cycle is encountered
        return func


def modules_in_class(model_class: Type[VertaModelBase]) -> List[ModuleType]:
    funcs = list_class_functions(model_class)
    modules_found = list()
    for function in funcs:
        _func = function[1]
        mods_in_def = list_modules_in_function_definition(_func)
        mods_in_args = list_modules_in_function_signature(_func)
        total_mods = mods_in_def + mods_in_args
        modules_found += total_mods
    return list(set(modules_found))