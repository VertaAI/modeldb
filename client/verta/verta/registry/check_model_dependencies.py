# -*- coding: utf-8 -*-
"""Utility function for checking model dependencies against an environment
to identify missing packages."""

from typing import Set, Type
import warnings

from verta.registry import VertaModelBase
from verta.environment import Python
from verta._internal_utils import model_dependencies as md
from verta._internal_utils._pip_requirements_utils import parse_req_spec


def check_model_dependencies(
        model_cls: Type[VertaModelBase],
        environment: Python,
        raise_for_missing: bool = False,
        ) -> bool:
    """Scan for missing dependencies in a model's environment.

    This function attempts to scan the provided model class for 3rd-party (not
    python standard library) dependencies and identify any packages in the
    provided environment.

    .. note::
        This function is not guaranteed to detect all dependencies in all cases.

    .. versionadded:: 0.22.2

    Parameters
    ----------
    model_cls: subclass of :class:`~verta.registry.VertaModelBase`
        Model class (not an instance) to be scanned.
    environment: :class:`~verta.environment.Python`
        Environment against which to validate pip dependencies.
    raise_for_missing: bool, default False
        If True, raises an exception if any dependencies detected in the model class
        are missing from the environment, or if the environment has extraneous
        packages.  Defaults to printing a warning.

    Returns
    -------
        bool
            True
                if all 3rd-party dependencies detected in the model class have
                corresponding packages in the environment.
            False
                if any 3rd-party dependencies detected in the model class are missing
                from the environment.

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
        check_model_dependencies(model=MyModelClass, environment=env)

    """
    detected_modules: Set[str] = md.class_module_names(model_cls)
    detected_packages: Set[str] = md.package_names(detected_modules)
    env_packages: Set[str] = { parse_req_spec(e)[0] for e in environment.requirements }
    error_msg = "the following packages are required by the model but missing " \
                "from the environment: "

    if detected_packages != env_packages:
        missing_packages = detected_packages - env_packages
        if missing_packages:
            if raise_for_missing:
                raise RuntimeError(error_msg + str(missing_packages))
            else:
                warnings.warn(
                    error_msg + str(missing_packages),
                    category=RuntimeWarning,
                )
                return False
    return True
