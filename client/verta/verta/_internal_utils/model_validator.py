# -*- coding: utf-8 -*-

import inspect
import warnings

from verta.registry import VertaModelBase
from verta.registry._verify_io import _DECORATED_FLAG

from . import importer


def must_keras(model):
    # TODO: figure out how to check if the model is from the Functional API
    # tensorflow.python.keras.engine.functional.Functional doesn't appear to
    # exist in all versions of tensorflow

    keras = importer.maybe_dependency("tensorflow.keras")
    if keras is None:
        raise TypeError("TensorFlow is not installed;" " try `pip install tensorflow`")
    if not isinstance(model, (keras.Sequential, keras.Model)):
        raise TypeError(
            "model must be either a Keras Sequential or Functional model,"
            " not {}".format(type(model))
        )

    return True


def must_sklearn(model):
    sklearn_base = importer.maybe_dependency("sklearn.base")
    if sklearn_base is None:
        raise TypeError(
            "scikit-learn is not installed;" " try `pip install scikit-learn`"
        )
    if not (
        isinstance(model, sklearn_base.BaseEstimator) and hasattr(model, "predict")
    ):
        raise TypeError(
            "model must be a scikit-learn estimator with a predict() method,"
            " not {}".format(type(model))
        )

    return True


def must_torch(model):
    torch_nn = importer.maybe_dependency("torch.nn")
    if torch_nn is None:
        raise TypeError("torch is not installed;" " try `pip install torch`")
    if not isinstance(model, torch_nn.Module):
        raise TypeError("model must be a torch.nn.Module, not {}".format(type(model)))

    return True


def must_xgboost_sklearn(model):
    xgboost_sklearn = importer.maybe_dependency("xgboost.sklearn")
    if xgboost_sklearn is None:
        raise TypeError("xgboost is not installed;" " try `pip install xgboost`")
    if not isinstance(model, xgboost_sklearn.XGBModel):
        raise TypeError(
            "model must be from XGBoost's scikit-learn API,"
            " not {}".format(type(model))
        )

    return True


def must_verta(model):
    if not (isinstance(model, type) and issubclass(model, VertaModelBase)):
        raise TypeError(
            "model must be a subclass of verta.registry.VertaModelBase,"
            " not {}".format(model)
        )

    remaining_abstract_methods = list(sorted(getattr(model, "__abstractmethods__", [])))
    if remaining_abstract_methods:
        raise TypeError(
            "model must finish implementing the following methods of"
            " VertaModelBase: {}".format(remaining_abstract_methods)
        )

    # model service passes __init__(artifacts) by keyword, so params must match
    expected_init_params = list(
        inspect.signature(VertaModelBase.__init__).parameters.keys()
    )
    init_params = list(inspect.signature(model.__init__).parameters.keys())
    if init_params != expected_init_params:
        raise TypeError(
            "model __init__() parameters must be {},"
            " not {}".format(expected_init_params, init_params)
        )

    if not getattr(model.predict, _DECORATED_FLAG, False):
        warnings.warn(
            "model predict() is not decorated with verta.registry.verify_io;"
            " argument and return types may change unintuitively when deployed"
        )

    expected_test_params = list(
        inspect.signature(VertaModelBase.model_test).parameters.keys()
    )
    test_params = list(inspect.signature(model.model_test).parameters.keys())
    if test_params != expected_test_params:
        raise TypeError(
            "model model_test() parameters must be {},"
            " not {}".format(expected_test_params, test_params)
        )

    return True
