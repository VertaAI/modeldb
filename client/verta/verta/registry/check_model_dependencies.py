# -*- coding: utf-8 -*-

from typing import Type

from verta.registry import VertaModelBase
from verta.environment import Python
from verta._internal_utils import model_dependencies as md


def check_model_dependencies(
        model: Type[VertaModelBase],
        environment: Python,
        raise_for_missing: bool = False,
        ) -> bool:
    """
    Attempt to scan the provided model class for dependencies and verify that
    the provided environment includes the necessary packages to support them.

    .. version_added:: 0.22.1

    Parameters
    ----------
    model: Type[VertaModelBase]
        Model class, inherited from :class:`~verta.registry.VertaModelBase` to be scanned.
    environment:
        Instance of `~verta.environment.Python` to validate against.
    raise_for_missing: bool, default False
        If True, raises an exception if any dependencies detected in the model class
        are missing from the environment. Defaults to printing a warning.

    Returns
    -------
    True if all dependencies detected are found in the environment, False otherwise.

    Raises
    ------
    RuntimeError
        If `raise_for_missing` is True and any dependencies are missing.

    """
    pass
