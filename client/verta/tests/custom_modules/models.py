# -*- coding: utf-8 -*-

# NOTE: Don't import pytest, or anything other than std lib and verta;
#       otherwise unpickling will try to find them and probably fail.
import importlib

from verta.registry import VertaModelBase, verify_io


def create_custom_module_model(module_name):
    """Return a model that imports `module_name` and predicts back its name.

    Parameters
    ----------
    module_name : str
        Name of a pip-installable module.

    Returns
    -------
    cls

    """

    class Model(VertaModelBase):
        def __init__(self, artifacts=None):
            exec("import {}".format(module_name), {})

        @verify_io
        def predict(self, input):
            return importlib.import_module(module_name).__name__

    return Model
