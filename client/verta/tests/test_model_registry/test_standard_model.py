# -*- coding: utf-8 -*-

"""ModelVersion.create_version_from_*() methods"""

import pytest

from verta.environment import Python
from verta._internal_utils import model_validator

from ..models import verta_model


def verta_models():
    models = []

    models.append(verta_model.VertaModel)

    return models


def keras_models():
    keras = pytest.importorskip("tensorflow.keras")

    models = []

    # sequential API
    models.append(
        keras.Sequential(
            [
                keras.layers.Dense(3, activation="linear", name="layer1"),
                keras.layers.Dense(2, activation="relu", name="layer2"),
                keras.layers.Dense(1, activation="sigmoid", name="layer3"),
            ]
        )
    )

    # functional API
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
    np = pytest.importorskip("numpy")
    xgb = pytest.importorskip("xgboost")

    models = []

    model = xgb.XGBClassifier(use_label_encoder=False)
    model.fit(
        np.random.random(size=(3, 3)),
        [0, 0, 1],
    )
    models.append(model)

    return models


def unsupported_xgboost_models():
    datasets = pytest.importorskip("sklearn.datasets")
    xgb = pytest.importorskip("xgboost")

    models = []

    # from https://xgboost.readthedocs.io/en/latest/python/model.html
    X, y = datasets.make_classification(
        n_samples=100, n_informative=5, n_classes=3,
    )
    dtrain = xgb.DMatrix(data=X, label=y)
    models.append(
        xgb.train(
            {
                "num_parallel_tree": 4,
                "subsample": 0.5,
                "num_class": 3,
            },
            num_boost_round=16,
            dtrain=dtrain,
        )
    )

    return models


class TestModelValidator:
    """verta._internal_utils.model_validator"""

    @pytest.mark.parametrize(
        "model",
        verta_models(),
    )
    def test_verta(self, model):
        assert model_validator.must_verta(model)

    @pytest.mark.parametrize(
        "model",
        keras_models() + sklearn_models() + torch_models() + xgboost_models(),
    )
    def test_not_verta(self, model):
        msg_match = r"^model must be a subclass of verta.registry.VertaModelBase.*"
        with pytest.raises(TypeError, match=msg_match):
            model_validator.must_verta(model)

    @pytest.mark.parametrize(
        "model",
        keras_models(),
    )
    def test_keras(self, model):
        assert model_validator.must_keras(model)

    @pytest.mark.parametrize(
        "model",
        verta_models() + sklearn_models() + torch_models() + xgboost_models(),
        # TODO: figure out how to detect unsupported_keras_models()
    )
    def test_not_keras(self, model):
        msg_match = r"^model must be either a Keras Sequential or Functional model.*"
        with pytest.raises(TypeError, match=msg_match):
            model_validator.must_keras(model)

    @pytest.mark.parametrize(
        "model",
        sklearn_models(),
    )
    def test_sklearn(self, model):
        assert model_validator.must_sklearn(model)

    @pytest.mark.parametrize(
        "model",
        unsupported_sklearn_models() + verta_models() + keras_models() + torch_models(),
        # xgboost_models() works because it uses a scikit-learn interface
    )
    def test_not_sklearn(self, model):
        msg_match = r"^model must be a scikit-learn estimator with a predict\(\) method.*"
        with pytest.raises(TypeError, match=msg_match):
            model_validator.must_sklearn(model)

    @pytest.mark.parametrize(
        "model",
        torch_models(),
    )
    def test_torch(self, model):
        assert model_validator.must_torch(model)

    @pytest.mark.parametrize(
        "model",
        verta_models() + keras_models() + sklearn_models() + xgboost_models(),
    )
    def test_not_torch(self, model):
        msg_match = r"^model must be a torch.nn.Module.*"
        with pytest.raises(TypeError, match=msg_match):
            model_validator.must_torch(model)

    @pytest.mark.parametrize(
        "model",
        xgboost_models(),
    )
    def test_xgboost(self, model):
        assert model_validator.must_xgboost_sklearn(model)

    @pytest.mark.parametrize(
        "model",
        verta_models() + keras_models() + sklearn_models() + torch_models() + unsupported_xgboost_models(),
    )
    def test_not_xgboost(self, model):
        msg_match = r"^model must be from XGBoost's scikit-learn API.*"
        with pytest.raises(TypeError, match=msg_match):
            model_validator.must_xgboost_sklearn(model)


class TestStandardModels:
    @pytest.mark.parametrize(
        "model",
        verta_models(),
    )
    def test_verta(self, registered_model, endpoint, model):
        artifact_value = [{"a": 1}]

        model_ver = registered_model.create_standard_model(
            model,
            Python([]),
            artifacts={model.ARTIFACT_KEY: artifact_value},
        )

        endpoint.update(model_ver, wait=True)
        deployed_model = endpoint.get_deployed_model()
        assert deployed_model.predict(artifact_value) == artifact_value

    @pytest.mark.parametrize(
        "model",
        keras_models() + sklearn_models() + torch_models() + xgboost_models(),
    )
    def test_not_verta(self, registered_model, model):
        with pytest.raises(TypeError):
            registered_model.create_standard_model(
                model,
                Python([]),
            )

    @pytest.mark.parametrize(
        "model",
        keras_models(),
    )
    def test_keras(self, registered_model, endpoint, model):
        np = pytest.importorskip("numpy")

        model_ver = registered_model.create_standard_model_from_keras(
            model,
            Python(["tensorflow"]),
        )

        endpoint.update(model_ver, wait=True)
        deployed_model = endpoint.get_deployed_model()
        assert deployed_model.predict(np.random.random(size=(3, 3)))

    @pytest.mark.parametrize(
        "model",
        verta_models() + unsupported_keras_models() + sklearn_models() + torch_models() + xgboost_models(),
    )
    def test_not_keras(self, registered_model, model):
        with pytest.raises(TypeError):
            registered_model.create_standard_model_from_keras(
                model,
                Python(["tensorflow"]),
            )

    @pytest.mark.parametrize(
        "model",
        sklearn_models(),
    )
    def test_sklearn(self, registered_model, endpoint, model):
        np = pytest.importorskip("numpy")

        model_ver = registered_model.create_standard_model_from_sklearn(
            model,
            Python(["scikit-learn"]),
        )

        endpoint.update(model_ver, wait=True)
        deployed_model = endpoint.get_deployed_model()
        assert deployed_model.predict(np.random.random(size=(3, 3)))

    @pytest.mark.parametrize(
        "model",
        unsupported_sklearn_models() + verta_models() + keras_models() + torch_models(),
        # xgboost_models() works because it uses a scikit-learn interface
    )
    def test_not_sklearn(self, registered_model, model):
        with pytest.raises(TypeError):
            registered_model.create_standard_model_from_sklearn(
                model,
                Python(["scikit-learn"]),
            )

    @pytest.mark.parametrize(
        "model",
        torch_models(),
    )
    def test_torch(self, registered_model, endpoint, model):
        np = pytest.importorskip("numpy")

        model_ver = registered_model.create_standard_model_from_torch(
            model,
            Python(["torch"]),
        )

        endpoint.update(model_ver, wait=True)
        deployed_model = endpoint.get_deployed_model()
        assert deployed_model.predict(np.random.random(size=(3, 3)))

    @pytest.mark.parametrize(
        "model",
        verta_models() + keras_models() + sklearn_models() + xgboost_models(),
    )
    def test_not_torch(self, registered_model, model):
        with pytest.raises(TypeError):
            registered_model.create_standard_model_from_torch(
                model,
                Python(["torch"]),
            )

    @pytest.mark.parametrize(
        "model",
        xgboost_models(),
    )
    def test_xgboost(self, registered_model, endpoint, model):
        np = pytest.importorskip("numpy")

        model_ver = registered_model.create_standard_model_from_xgboost(
            model,
            Python(["xgboost"]),
        )

        endpoint.update(model_ver, wait=True)
        deployed_model = endpoint.get_deployed_model()
        assert deployed_model.predict(np.random.random(size=(3, 3)))

    @pytest.mark.parametrize(
        "model",
        verta_models() + keras_models() + sklearn_models() + torch_models() + unsupported_xgboost_models(),
    )
    def test_not_xgboost(self, registered_model, model):
        with pytest.raises(TypeError):
            registered_model.create_standard_model_from_xgboost(
                model,
                Python(["xgboost"]),
            )
