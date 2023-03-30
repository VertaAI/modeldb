# -*- coding: utf-8 -*-

import inspect
from importlib_metadata import packages_distributions
from types import ModuleType
from typing import Any, Callable, Dict, get_type_hints, List, Set, Type

from ..registry._verta_model_base import VertaModelBase


def class_functions(model_class: Type[VertaModelBase]) -> Set[Callable]:
    """Return a set of all the functions present in the provided class object."""
    return set(
        [f[1] for f in inspect.getmembers(model_class, predicate=inspect.isfunction)]
    )


def unwrap(func: Callable) -> Callable:
    """Unwraps a function and returns the original function object in a
    non-blocking manner."""
    try:
        return inspect.unwrap(func)  # returns last object in chain
    except ValueError:  # if a cycle is encountered
        return func


def modules_in_function_body(func: Callable) -> Set[str]:
    """Return a set of all base modules called within the body of the provided function.
    The `unbound` value returned by `inspect.getclosurevars` returns a set of names
    referenced in the function that could not be resolved.
    """
    _func = unwrap(func)
    function_variables: List[Dict[str, Any]] = [
        val
        for key, val in inspect.getclosurevars(_func)._asdict().items()
        if key != "unbound"
    ]
    modules = set()
    for variable in function_variables:
        for object in variable.values():
            module: ModuleType = inspect.getmodule(object)
            module_name_with_submodules: str = module.__name__
            base_module_name: str = module_name_with_submodules.split(".")[0]
            modules.add(base_module_name)
    return modules


def modules_in_function_signature(func: Callable) -> Set[str]:
    """Return a set all base modules used in type hints in the provided
    function's arguments and return type hint."""
    _func = unwrap(func)
    hints = get_type_hints(_func)
    arg_hints = {k: v for k, v in hints.items() if k != "return"}
    return_hint = hints.get("return")

    modules = [inspect.getmodule(value) for value in arg_hints.values()]

    if return_hint:
        mod = inspect.getmodule(return_hint)
        if mod.__name__ == "typing":
            ret_ann = inspect.signature(_func).return_annotation
            nested_args = ret_ann.__args__
            for a in nested_args:
                if inspect.isclass(a):
                    modules.append(inspect.getmodule(a))
        else:
            modules.append(inspect.getmodule(return_hint))
    return set([m.__name__.split(".")[0] for m in modules])


def class_module_names(model_class: Type[VertaModelBase]) -> Set[str]:
    """Return a set of all base modules detected in the provided class object."""
    modules_found = set()
    for function in class_functions(model_class):
        _func = function
        mods_in_body = modules_in_function_body(_func)
        mods_in_signature = modules_in_function_signature(_func)
        modules_found |= mods_in_body | mods_in_signature
    return modules_found


def package_names(module_names: Set[str]) -> Dict[str, List[str]]:
    """Return a dictionary where the key is the name of the import module
    and the value is the list of possible distribution packages for each
    3rd-party import module in the provided set that can be found in the
    locally installed package_distributions.
    """
    pkg_dist = packages_distributions()
    return {m: pkg_dist.get(m) for m in module_names if pkg_dist.get(m)}
    # TODO: handle cases where 3rd-party module is not found in pkg_dist
