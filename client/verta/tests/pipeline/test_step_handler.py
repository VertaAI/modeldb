# -*- coding: utf-8 -*-

from verta._pipeline_orchestrator._step_handler import ModelObjectStepHandler
from verta.environment import Python

from tests.models.standard_models import VertaModel


class TestModelObjectStepHandler:
    def test_init_model(self, registered_model):
        """Verify _init_model() instantiates model as expected."""
        artifact = 123
        model_ver = registered_model.create_standard_model(
            VertaModel,
            artifacts={VertaModel.ARTIFACT_KEY: artifact},
            code_dependencies=[],
            environment=Python([]),
        )

        model_ver_model = model_ver.get_model()(
            model_ver.fetch_artifacts([VertaModel.ARTIFACT_KEY]),
        )
        step_handler_model = ModelObjectStepHandler._init_model(
            model_ver._conn,
            model_ver.id,
        )

        assert (
            artifact
            == model_ver_model.predict(None)
            == step_handler_model.predict(None)
        )
