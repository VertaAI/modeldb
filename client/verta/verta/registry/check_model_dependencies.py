# -*- coding: utf-8 -*-
"""Utility function for checking model dependencies against an environment
to identify missing packages."""

from typing import Set, Type
import warnings

from verta.registry import VertaModelBase
from verta.environment import Python
from verta._internal_utils import model_dependencies as md


def check_model_dependencies(
        model: Type[VertaModelBase],
        environment: Python,
        raise_for_missing: bool = False,
        ) -> bool:
    """Scan for missing or unused dependencies in a model's environment
    to help avoid deployment failures and reduce time to deploy

    This function attempts to scan the provided model class for 3rd-party (not
    python standard library) dependencies and identify any missing or
    extraneous packages in the provided environment.

    .. note::
        This function is not guaranteed to detect all dependencies in all cases.

    .. versionadded:: 0.22.2

    Parameters
    ----------
    model: subclass of :class:`~verta.registry.VertaModelBase`
        Model class object (not an instance) to be scanned.
    environment:
        Instance of a :class:`~verta.environment.Python` environment against
        which to validate dependencies.
    raise_for_missing: bool, default False
        If True, raises an exception if any dependencies detected in the model class
        are missing from the environment, or if the environment has extraneous
        packages.  Defaults to printing a warning.

    Returns
    -------
        True
            if all 3rd-party dependencies detected in the model class have
            corresponding packages in the environment, and no extraneous packages
            exist in the environment.
        False
            if any 3rd-party dependencies detected in the model class are missing
            or extraneous packages are detected in the environment.

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
    detected_modules: Set[str] = md.class_module_names(model)
    detected_packages: Set[str] = md.package_names(detected_modules)
    env_packages: Set[str] = { md.strip_version(r) for r in environment.requirements }

    if detected_packages !=  env_packages:
        missing_packages = detected_packages - env_packages
        extra_packages = env_packages - detected_packages
        if missing_packages:
            if raise_for_missing:
                raise RuntimeError(
                    f"the following packages are required by the model but missing "
                    f"from the environment: {missing_packages}"
                )
            else:
                warnings.warn(
                    f"the following packages are required by the model but missing "
                    f"from the environment: {missing_packages}",
                    category=RuntimeWarning,
                )
                return False

        if extra_packages:
            if raise_for_missing:
                raise RuntimeError(
                    f"the following packages are not required by the model but are present "
                    f"in the environment: {extra_packages}"
                )
            else:
                warnings.warn(
                    f"the following packages are not required by the model but are present "
                    f"in the environment: {extra_packages}",
                    category=RuntimeWarning,
                )
                return False
    return True
