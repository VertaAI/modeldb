# -*- coding: utf-8 -*-
"""Utility function for checking model dependencies against an environment
to identify missing packages."""

import warnings
from typing import Any, Dict, List, Set, Type

from verta._internal_utils import model_dependencies as md
from verta._internal_utils._pip_requirements_utils import parse_req_spec
from verta.environment import Python
from ._verta_model_base import VertaModelBase


def _check_model_dependencies(
        model_cls: Type[VertaModelBase],
        environment: Python,
        raise_for_missing: bool = False,
) -> bool:
    """Scan for missing dependencies in a model's environment.

    This function attempts to scan the provided model class for 3rd-party (not
    python standard library) dependencies and identify any missing packages in the
    provided environment.

    .. note::
        This function is not guaranteed to detect all dependencies in all cases,
        and should be considered a fast sanity check rather than a test.

    .. versionadded:: 0.22.2

    Parameters
    ----------
    model_cls: subclass of :class:`~verta.registry.VertaModelBase`
        Model class (not an instance) to be scanned.
    environment: :class:`~verta.environment.Python`
        Environment against which to validate pip dependencies.
    raise_for_missing: bool, default False
        If `True`, raises an exception if any dependencies detected in the model class
        are missing from the environment.  Defaults to printing a warning.

    Returns
    -------
    bool
        `True` if all 3rd-party dependencies detected in the model class have
        corresponding packages in the environment. `False` if any are missing.

    Raises
    ------
    RuntimeError
        If `raise_for_missing` is True and any dependencies are extraneous or missing.

    Examples
    --------
    .. code-block:: python
       :emphasize-lines: 5

        from verta.registry import check_model_dependencies
        from verta.environment import Python

        env = Python(["numpy", "pandas"])
        check_model_dependencies(model_cls=MyModelClass, environment=env)

    """
    env_packages: Set[str] = {parse_req_spec(e)[0] for e in environment.requirements}
    detected_modules: Set[str] = md.class_module_names(model_cls)
    detected_packages: Dict[str, List[str]] = md.package_names(detected_modules)

    missing_packages: Dict[str, List[str]] = {
        mod: pkgs
        for mod, pkgs in detected_packages.items()
        if not env_packages.intersection(set(pkgs))
           and mod not in env_packages  # namespace package
    }

    if missing_packages:
        formatted_missing_packages: List[Dict[str, List[str]]] = [
            {'import_module': mod, 'distribution_packages': pkgs}
            for mod, pkgs in missing_packages.items()
        ]
        error_msg = f"the following import modules are required by the model but missing " \
                    f"any corresponding distribution package in the environment:"
        for m in formatted_missing_packages:
            error_msg += f"\n{m}"
        if raise_for_missing:
            raise RuntimeError(error_msg)
        warnings.warn(
            error_msg,
            category=RuntimeWarning,
        )
        return False
    return True
