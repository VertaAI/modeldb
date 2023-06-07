# -*- coding: utf-8 -*-

import abc
import json
from typing import Any, Dict, List, Optional, Type

from verta._internal_utils import _utils, http_session
from verta.registry.entities import RegisteredModelVersion
from verta.tracking.entities._entity import _MODEL_ARTIFACTS_ATTR_KEY


class _StepHandlerBase(abc.ABC):
    def __init__(
        self,
        name: str,
        model_version_id: int,
    ):
        self._name = name
        self._model_version_id = model_version_id

    @abc.abstractmethod
    def run(self, input: Dict[str, Any]) -> Dict[str, Any]:
        raise NotImplementedError

    @property
    def name(self) -> str:
        return self._name


class ModelContainerStepHandler(_StepHandlerBase):
    def __init__(
        self,
        name: str,
        model_version_id: int,
        prediction_url: str,
    ):
        super().__init__(name=name, model_version_id=model_version_id)
        self._session = http_session.init_session(retry=http_session.retry_config())
        self._prediction_url = prediction_url

    def run(self, input: Dict[str, Any]) -> Dict[str, Any]:
        body = json.dumps(
            _utils.to_builtin(input),
            allow_nan=True,
        )
        response = self._session.post(
            self._prediction_url,
            data=body,
        )
        _utils.raise_for_http_error(response)
        return response.json()


class ModelObjectStepHandler(_StepHandlerBase):
    def __init__(
        self,
        name: str,
        conn: _utils.Connection,
        model_version_id: int,
    ):
        super().__init__(name=name, model_version_id=model_version_id)
        self._model = self._init_model(conn)

    def _init_model(self, conn: _utils.Connection) -> Any:
        model_ver = RegisteredModelVersion._get_by_id(
            conn,
            _utils.Configuration(),
            self._model_version_id,
        )

        model_cls: Type[Any] = model_ver.get_model()

        model_artifacts: Optional[Dict[str, str]] = None
        model_artifacts_keys: Optional[List[str]] = model_ver.get_attributes().get(
            _MODEL_ARTIFACTS_ATTR_KEY,
        )
        if model_artifacts_keys is not None:
            model_artifacts = model_ver.fetch_artifacts(model_artifacts_keys)

        return model_cls(artifacts=model_artifacts)

    def run(self, input: Dict[str, Any]) -> Dict[str, Any]:
        return self._model.predict(input)
