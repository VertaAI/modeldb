# -*- coding: utf-8 -*-

import pickle

import pytest

from verta.registry import VertaModelBase
from verta._internal_utils.importer import get_tensorflow_major_version


class VertaModel(VertaModelBase):
    ARTIFACT_KEY = "artifact"

    def __init__(self, artifacts):
        with open(artifacts[self.ARTIFACT_KEY], "rb") as f:
            self.artifact = pickle.load(f)

    def predict(self, input):
        return self.artifact


def verta_models():
    models = []

    models.append(VertaModel)

    return models


def keras_models():
    keras = pytest.importorskip("tensorflow.keras")

    models = []

    # TODO: re-enable with VR-11964
    # # sequential API
    # model = keras.Sequential(
    #     [
    #         keras.layers.Dense(3, activation="linear", name="layer1"),
    #         keras.layers.Dense(2, activation="relu", name="layer2"),
    #         keras.layers.Dense(1, activation="sigmoid", name="layer3"),
    #     ]
    # )
    # model(np.random.random(size=(3, 3)))  # initialize weights
    # models.append(model)

    # functional API
    # TODO: re-enable for TF 1.X with VR-12011
    if get_tensorflow_major_version() != 1:
        inputs = keras.Input(shape=(3,))
        x = keras.layers.Dense(2, activation="relu")(inputs)
        outputs = keras.layers.Dense(1, activation="sigmoid")(x)
        models.append(keras.Model(inputs=inputs, outputs=outputs))

    return models


def unsupported_keras_models():
    keras = pytest.importorskip("tensorflow.keras")

    models = []

    # subclassing API
    class MyModel(keras.Model):
        def __init__(self):
            super(MyModel, self).__init__()
            self.layer1 = keras.layers.Dense(3, activation="linear")
            self.layer2 = keras.layers.Dense(2, activation="relu")
            self.layer3 = keras.layers.Dense(2, activation="sigmoid")

        def call(self, inputs):
            x = self.layer1(inputs)
            x = self.layer2(x)
            return self.layer3(x)

    models.append(MyModel())

    return models


def sklearn_models():
    np = pytest.importorskip("numpy")
    ensemble = pytest.importorskip("sklearn.ensemble")
    svm = pytest.importorskip("sklearn.svm")

    models = []

    model = ensemble.RandomForestClassifier()
    model.fit(
        np.random.random(size=(3, 3)),
        [0, 0, 1],
    )
    models.append(model)

    model = svm.LinearSVC()
    model.fit(
        np.random.random(size=(3, 3)),
        [0, 0, 1],
    )
    models.append(model)

    return models


def unsupported_sklearn_models():
    preprocessing = pytest.importorskip("sklearn.preprocessing")

    models = []

    models.append(preprocessing.Normalizer())

    return models


def torch_models():
    torch = pytest.importorskip("torch")

    models = []

    # subclass API
    class Model(torch.nn.Module):
        def __init__(self):
            super(Model, self).__init__()
            self.layer1 = torch.nn.Linear(3, 2)
            self.layer2 = torch.nn.Linear(2, 1)

        def forward(self, x):
            x = torch.nn.functional.relu(self.layer1(x))
            return torch.sigmoid(self.layer2(x))

    models.append(Model())

    # sequential API
    models.append(
        torch.nn.Sequential(
            torch.nn.Linear(3, 2),
            torch.nn.ReLU(),
            torch.nn.Linear(2, 1),
            torch.nn.Sigmoid(),
        )
    )

    return models


def xgboost_models():
    # TODO: re-enable with VR-11963
    # np = pytest.importorskip("numpy")
    # xgb = pytest.importorskip("xgboost")

    models = []

    # model = xgb.XGBClassifier(use_label_encoder=False)
    # model.fit(
    #     np.random.random(size=(3, 3)),
    #     [0, 0, 1],
    # )
    # models.append(model)

    return models


def unsupported_xgboost_models():
    # TODO: re-enable with VR-11963
    # datasets = pytest.importorskip("sklearn.datasets")
    # xgb = pytest.importorskip("xgboost")

    models = []

    # # from https://xgboost.readthedocs.io/en/latest/python/model.html
    # X, y = datasets.make_classification(
    #     n_samples=100,
    #     n_informative=5,
    #     n_classes=3,
    # )
    # dtrain = xgb.DMatrix(data=X, label=y)
    # models.append(
    #     xgb.train(
    #         {
    #             "num_parallel_tree": 4,
    #             "subsample": 0.5,
    #             "num_class": 3,
    #         },
    #         num_boost_round=16,
    #         dtrain=dtrain,
    #     )
    # )

    return models
