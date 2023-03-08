# -*- coding: utf-8 -*-

import inspect
from importlib.metadata import distribution, packages_distributions
from types import ModuleType
from typing import Callable, get_type_hints, List, Set, Type, Union

from ..registry._verta_model_base import VertaModelBase


def class_functions(model_class: Type[VertaModelBase]) -> Set[Callable]:
    """Return a set of all the functions present in the provided class object."""
    return set([
        f[1] for f in inspect.getmembers(model_class, predicate=inspect.isfunction)
    ])


def unwrap(func: Callable) -> Callable:
    """Unwraps a function and returns the original function object in a
    non-blocking manner."""
    try:
        return inspect.unwrap(func)  # returns last object in chain
    except ValueError:  # if a cycle is encountered
        return func


def modules_in_function_body(func: Callable) -> Set[str]:
    """Return a set of all base modules called within the body of the provided function."""
    _func = unwrap(func)
    _globals = [
        value.__name__.split('.')[0]  # strip off submodules and classes
        for key, value in inspect.getclosurevars(_func).globals.items()
        if isinstance(value, ModuleType)
    ]
    _non_locals = [
       value.__name__.split('.')[0]  # strip off submodules and classes
       for key, value in inspect.getclosurevars(_func).nonlocals.items()
       if isinstance(value, ModuleType)
    ]
    return set(_globals + _non_locals)


def modules_in_function_signature(func: Callable) -> Set[str]:
    """Return a set all base modules used in type hints in the provided
    function's arguments and return type hint."""
    _func = unwrap(func)
    hints = get_type_hints(_func)
    arg_hints = { k: v for k, v in hints.items() if k != 'return' }
    return_hint = hints.get('return')

    modules = [ inspect.getmodule(value) for value in arg_hints.values() ]

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
    return  set([ m.__name__.split('.')[0] for m in modules ])


def class_module_names(model_class: Type[VertaModelBase]) -> Set[str]:
    """Return a set of all base modules detected in the provided class object."""
    modules_found = set()
    for function in class_functions(model_class):
        _func = function
        mods_in_body = modules_in_function_body(_func)
        mods_in_signature = modules_in_function_signature(_func)
        modules_found |= mods_in_body | mods_in_signature
    return modules_found


def package_names(module_names: Set[str]) -> Set[str]:
    """Return a set of distribution package names for all 3rd-party import
    modules in the provided set.

    Package distributions don't exist for modules in the Python standard library.
    This fact is used to capture only 3rd-party packages.  Modules associated
    with a single package name are easily captured.  Modules associated with a
    namespace package (e.g.`google`), may come from a variety of distribution
    packages.  To avoid making an arbitrary choice in those cases, the main
    namespace package name is fetched from the distribution metadata.
    """
    pkg_dist = packages_distributions()
    pkgs = set()
    for mod in module_names:
        dist_pkgs: Union[List[str], None] = pkg_dist.get(mod)
        if dist_pkgs:
            if len(dist_pkgs) == 1:
                pkgs.add(dist_pkgs[0])
            if len(dist_pkgs) > 1:
                pkgs.add(distribution(mod).name)
    return pkgs
