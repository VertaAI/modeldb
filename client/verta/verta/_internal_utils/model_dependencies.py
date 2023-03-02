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


def list_modules_in_function_body(func: Callable) -> List[ModuleType]:
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
    return_hint = hints.get('return')

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


def list_function_local_annotation_module_names(func: Callable) -> List[str]:
    """Return a list of modules extracted from type annotations on function
    locals.

    Variable annotations within a function body do not have a lifetime beyond
    the local namespace. This function extracts the annotations from the
    function body when possible.

    """
    class AnnotationsCollector(ast.NodeVisitor):
        """Collects AnnAssign nodes for 'simple' annotation assignments"""

        def __init__(self):
            self.annotations = {}

        def visit_AnnAssign(self, node) -> None:
            if node.simple:
                # 'simple' == a single name, not an attribute or subscription, thus
                # `node.target.id` should exist.
                self.annotations[node.target.id] = node.annotation

    source = textwrap.dedent(inspect.getsource(func))
    mod = ast.parse(source)
    collector = AnnotationsCollector()
    collector.visit(mod.body[0])
    return [
        ast.get_source_segment(source, node)
        for node in collector.annotations.values()
    ]


def module_names_in_class(model_class: Type[VertaModelBase]) -> List[str]:
    funcs = list_class_functions(model_class)
    modules_found = list()
    for function in funcs:
        _func = function[1]
        mods_in_def = [
            m.__name__.split('.')[0]
            for m in list_modules_in_function_body(_func)
        ]
        mods_in_args = [
            m.__name__.split('.')[0]
            for m in list_modules_in_function_signature(_func)
        ]
        mods_in_anns = [
            m.split('.')[0]
            for m in list_function_local_annotation_module_names(_func)
        ]
        modules_found += mods_in_def + mods_in_args + mods_in_anns
    return modules_found