# -*- coding: utf-8 -*-

import ast
import inspect
import re
from importlib.metadata import packages_distributions
from typing import Callable, List, Set, Tuple, Type, get_type_hints
from types import ModuleType
from verta.registry import VertaModelBase


def list_class_functions(model_class: Type[VertaModelBase]) -> List[Tuple[str, Callable]]:
    """List all the functions present in the provided class object."""
    return inspect.getmembers(model_class, predicate=inspect.isfunction)


def list_modules_in_function_definition(func: Callable) -> List[ModuleType]:
    """List all modules called within the body of the provided function"""
    if is_wrapped(func):
        pass
        # unwrap here
    return [
        value for key, value in inspect.getclosurevars(func).globals.items()
        if isinstance(value, ModuleType)
    ]

def is_wrapped(func: Callable, **kwargs) -> bool:
    try:
        return func.__wrapped__(**kwargs) is not None
    except AttributeError:
        return False