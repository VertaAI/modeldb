# -*- coding: utf-8 -*-

import abc
import json
from typing import Any, Dict, Type

from verta._internal_utils import _utils, http_session


class _StepHandlerBase(abc.ABC):
    def __init__(self, name: str):
        self._name = name

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
        prediction_url: str,
    ):
        super().__init__(name)
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
        model_cls: Type[Any],
        model_artifacts: Dict[str, Any],
    ):
        super().__init__(name)
        self._model = model_cls(model_artifacts)

    def run(self, input: Dict[str, Any]) -> Dict[str, Any]:
        return self._model.predict(input)
