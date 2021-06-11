# -*- coding: utf-8 -*-

from verta.registry import VertaModelBase

from . import importer


def check_keras(model):
    keras = importer.maybe_dependency("tensorflow.keras")
    keras_functional = importer.maybe_dependency(
        "tensorflow.python.keras.engine.functional",
    )
    if keras is None or keras_functional is None:
        raise TypeError("TensorFlow is not installed;" " try `pip install tensorflow`")
    if not isinstance(model, (keras.Sequential, keras_functional.Functional)):
        raise TypeError(
            "model must be either a Keras Sequential or Functional model,"
            " not {}".format(type(model))
        )

    return True


def check_sklearn(model):
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


def check_torch(model):
    torch_nn = importer.maybe_dependency("torch.nn")
    if torch_nn is None:
        raise TypeError("torch is not installed;" " try `pip install torch`")
    if not isinstance(model, torch_nn.Module):
        raise TypeError(
            "model must be a torch.nn.Module," " not {}".format(type(model))
        )

    return True


def check_xgboost_sklearn(model):
    xgboost_sklearn = importer.maybe_dependency("xgboost.sklearn")
    if xgboost_sklearn is None:
        raise TypeError("xgboost is not installed;" " try `pip install xgboost`")
    if not isinstance(model, xgboost_sklearn.XGBModel):
        raise TypeError(
            "model must be from XGBoost's scikit-learn API,"
            " not {}".format(type(model))
        )

    return True


def check_verta(model):
    if not issubclass(model, VertaModelBase):
        raise TypeError(
            "model must be a subclass of"
            " verta.registry.VertaModelBase, not"
            " {}".format(model)
        )

    return True
