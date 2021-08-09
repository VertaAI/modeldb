# -*- coding: utf-8 -*-

"""ModelVersion.create_version_from_*() methods"""

from datetime import timedelta
import re

import hypothesis
import hypothesis.strategies as st
import pytest

from verta._internal_utils import _artifact_utils, model_validator
from verta.environment import Python
from verta.external import six
from verta.registry import verify_io

from ..models import standard_models
from ..strategies import json_strategy


verta_models = standard_models.verta_models()
decorated_verta_models = standard_models.decorated_verta_models()
incomplete_verta_models = standard_models.incomplete_verta_models()
keras_models = standard_models.keras_models()
unsupported_keras_models = standard_models.unsupported_keras_models()
sklearn_models = standard_models.sklearn_models()
unsupported_sklearn_models = standard_models.unsupported_sklearn_models()
torch_models = standard_models.torch_models()
xgboost_models = standard_models.xgboost_models()
unsupported_xgboost_models = standard_models.unsupported_xgboost_models()


class TestVerifyIO:
    @hypothesis.settings(deadline=timedelta(milliseconds=50))
    @hypothesis.given(value=json_strategy)
    def test_verify_io_allow(self, value):
        @verify_io
        def predict(self, input):
            return input

        predict(None, value)

    def test_verify_io_reject(self):
        array = pytest.importorskip("numpy").array([1, 2, 3])
        df = pytest.importorskip("pandas").DataFrame([1, 2, 3])
        tensor = pytest.importorskip("torch").tensor([1, 2, 3])

        msg_match = "not JSON serializable.*{} must only contain types"
        for value in [array, df, tensor]:

            @verify_io
            def predict(self, _):
                return value
            with pytest.raises(TypeError, match=msg_match.format("input")):
                predict(None, value)
            with pytest.raises(TypeError, match=msg_match.format("output")):
                predict(None, None)

    def test_verify_io_reject_bytes(self):
        # TODO: create Hypothesis strategy for non-decodable binary
        value = b"\x80abc"

        if six.PY2:
            msg_match = "'utf8' codec can't decode byte.*{} cannot contain binary"
        else:
            msg_match = "not JSON serializable.*{} must only contain types"

        @verify_io
        def predict(self, _):
            return value
        with pytest.raises(TypeError, match=msg_match.format("input")):
            predict(None, value)
        with pytest.raises(TypeError, match=msg_match.format("output")):
            predict(None, None)


class TestModelValidator:
    """verta._internal_utils.model_validator"""

    @pytest.mark.parametrize(
        "model",
        verta_models,
    )
    def test_verta(self, model):
        msg_match = "^" + re.escape(
            "model predict() is not decorated with verta.registry.verify_io;"
        )
        with pytest.warns(UserWarning, match=msg_match):
            assert model_validator.must_verta(model)

    @pytest.mark.parametrize(
        "model",
        decorated_verta_models,
    )
    def test_decorated_verta(self, model):
        with pytest.warns(None) as record:
            model_validator.must_verta(model)
        assert not record  # no warning of missing decorator on predict()

    @pytest.mark.parametrize(
        "model",
        incomplete_verta_models,
    )
    def test_incomplete_verta(self, model):
        msg_match = (
            r"^model must finish implementing the following methods of VertaModelBase: "
        )
        msg_match += re.escape(str(list(sorted(model.__abstractmethods__))))
        with pytest.raises(TypeError, match=msg_match):
            model_validator.must_verta(model)

    @pytest.mark.parametrize(
        "model",
        keras_models + sklearn_models + torch_models + xgboost_models,
    )
    def test_not_verta(self, model):
        msg_match = r"^model must be a subclass of verta.registry.VertaModelBase.*"
        with pytest.raises(TypeError, match=msg_match):
            model_validator.must_verta(model)

    @pytest.mark.parametrize(
        "model",
        keras_models,
    )
    def test_keras(self, model):
        assert model_validator.must_keras(model)

    @pytest.mark.parametrize(
        "model",
        verta_models + sklearn_models + torch_models + xgboost_models,
        # TODO: figure out how to detect unsupported_keras_models
    )
    def test_not_keras(self, model):
        msg_match = r"^model must be either a Keras Sequential or Functional model.*"
        with pytest.raises(TypeError, match=msg_match):
            model_validator.must_keras(model)

    @pytest.mark.parametrize(
        "model",
        sklearn_models,
    )
    def test_sklearn(self, model):
        assert model_validator.must_sklearn(model)

    @pytest.mark.parametrize(
        "model",
        unsupported_sklearn_models + verta_models + keras_models + torch_models,
        # xgboost_models works because it uses a scikit-learn interface
    )
    def test_not_sklearn(self, model):
        msg_match = (
            r"^model must be a scikit-learn estimator with a predict\(\) method.*"
        )
        with pytest.raises(TypeError, match=msg_match):
            model_validator.must_sklearn(model)

    @pytest.mark.parametrize(
        "model",
        torch_models,
    )
    def test_torch(self, model):
        assert model_validator.must_torch(model)

    @pytest.mark.parametrize(
        "model",
        verta_models + keras_models + sklearn_models + xgboost_models,
    )
    def test_not_torch(self, model):
        msg_match = r"^model must be a torch.nn.Module.*"
        with pytest.raises(TypeError, match=msg_match):
            model_validator.must_torch(model)

    @pytest.mark.parametrize(
        "model",
        xgboost_models,
    )
    def test_xgboost(self, model):
        assert model_validator.must_xgboost_sklearn(model)

    @pytest.mark.parametrize(
        "model",
        verta_models
        + keras_models
        + sklearn_models
        + torch_models
        + unsupported_xgboost_models,
    )
    def test_not_xgboost(self, model):
        msg_match = r"^model must be from XGBoost's scikit-learn API.*"
        with pytest.raises(TypeError, match=msg_match):
            model_validator.must_xgboost_sklearn(model)


class TestStandardModels:
    @pytest.mark.parametrize(
        "model",
        verta_models,
    )
    def test_verta(self, registered_model, endpoint, model):
        artifact_value = [{"a": 1}]

        # blocklisted artifact keys raise error
        for key in _artifact_utils.BLOCKLISTED_KEYS:
            with pytest.raises(ValueError, match="please use a different key$"):
                registered_model.create_standard_model(
                    model,
                    Python([]),
                    artifacts={key: artifact_value},
                )

        msg_match = "^" + re.escape(
            "model predict() is not decorated with verta.registry.verify_io;"
        )
        with pytest.warns(UserWarning, match=msg_match):
            model_ver = registered_model.create_standard_model(
                model,
                Python(["pytest"]),  # source module imports pytest
                artifacts={model.ARTIFACT_KEY: artifact_value},
            )

        endpoint.update(model_ver, wait=True)
        deployed_model = endpoint.get_deployed_model()
        assert deployed_model.predict(artifact_value) == artifact_value

    @pytest.mark.parametrize(
        "model",
        decorated_verta_models,
    )
    def test_decorated_verta(self, registered_model, endpoint, model):
        np = pytest.importorskip("numpy")

        with pytest.warns(None) as record:
            model_ver = registered_model.create_standard_model(
                model,
                Python(["pytest"]),  # source module imports pytest
            )
        assert not record  # no warning of missing decorator on predict()

        endpoint.update(model_ver, wait=True)
        deployed_model = endpoint.get_deployed_model()
        assert deployed_model.predict(np.random.random(size=(3, 3)).tolist())

    @pytest.mark.parametrize(
        "model",
        incomplete_verta_models,
    )
    def test_incomplete_verta(self, registered_model, model):
        with pytest.raises(TypeError):
            registered_model.create_standard_model(
                model,
                Python([]),
            )

    @pytest.mark.parametrize(
        "model",
        keras_models + sklearn_models + torch_models + xgboost_models,
    )
    def test_not_verta(self, registered_model, model):
        with pytest.raises(TypeError):
            registered_model.create_standard_model(
                model,
                Python([]),
            )

    @pytest.mark.tensorflow
    @pytest.mark.parametrize(
        "model",
        keras_models,
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

    @pytest.mark.tensorflow
    @pytest.mark.parametrize(
        "model",
        verta_models
        + unsupported_keras_models
        + sklearn_models
        + torch_models
        + xgboost_models,
    )
    def test_not_keras(self, registered_model, model):
        with pytest.raises(
            (
                TypeError,
                NotImplementedError,  # Keras raises this for subclassing API
            )
        ):
            registered_model.create_standard_model_from_keras(
                model,
                Python(["tensorflow"]),
            )

    @pytest.mark.parametrize(
        "model",
        sklearn_models,
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
        unsupported_sklearn_models + verta_models + keras_models + torch_models,
        # xgboost_models works because it uses a scikit-learn interface
    )
    def test_not_sklearn(self, registered_model, model):
        with pytest.raises(TypeError):
            registered_model.create_standard_model_from_sklearn(
                model,
                Python(["scikit-learn"]),
            )

    @pytest.mark.parametrize(
        "model",
        torch_models,
    )
    def test_torch(self, registered_model, endpoint, model):
        np = pytest.importorskip("numpy")

        # TODO: find a more automatic way to do this for the user (VR-11973)
        reqs = ["torch"]
        if six.PY2:
            # Python 2 torch requires this to deserialize models
            reqs.append("future")

        model_ver = registered_model.create_standard_model_from_torch(
            model,
            Python(reqs),
        )

        endpoint.update(model_ver, wait=True)
        deployed_model = endpoint.get_deployed_model()
        assert deployed_model.predict(np.random.random(size=(3, 3)))

    @pytest.mark.parametrize(
        "model",
        verta_models + keras_models + sklearn_models + xgboost_models,
    )
    def test_not_torch(self, registered_model, model):
        with pytest.raises(TypeError):
            registered_model.create_standard_model_from_torch(
                model,
                Python(["torch"]),
            )

    @pytest.mark.parametrize(
        "model",
        xgboost_models,
    )
    def test_xgboost(self, registered_model, endpoint, model):
        np = pytest.importorskip("numpy")

        model_ver = registered_model.create_standard_model_from_xgboost(
            model,
            Python(["scikit-learn", "xgboost"]),
        )

        endpoint.update(model_ver, wait=True)
        deployed_model = endpoint.get_deployed_model()
        assert deployed_model.predict(np.random.random(size=(3, 3)))

    @pytest.mark.parametrize(
        "model",
        verta_models
        + keras_models
        + sklearn_models
        + torch_models
        + unsupported_xgboost_models,
    )
    def test_not_xgboost(self, registered_model, model):
        with pytest.raises(TypeError):
            registered_model.create_standard_model_from_xgboost(
                model,
                Python(["scikit-learn", "xgboost"]),
            )
