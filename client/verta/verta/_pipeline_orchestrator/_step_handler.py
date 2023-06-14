# -*- coding: utf-8 -*-

import abc
import json
from typing import Any, Dict, List, Optional, Set, Type

from verta._internal_utils import _utils, http_session
from verta.registry.entities import RegisteredModelVersion
from verta.tracking.entities._entity import _MODEL_ARTIFACTS_ATTR_KEY


class _StepHandlerBase(abc.ABC):
    """Abstract base class for inference pipeline step handlers."""

    def __init__(
        self,
        name: str,
        model_version_id: int,
        predecessors: Set[int],
    ):
        self._name = name
        self._model_version_id = model_version_id
        if len(predecessors) > 1:
            # TODO: figure out how to orchestrate complex pipelines
            raise ValueError("multiple inputs is not yet supported")
        self._predecessors = predecessors

    @abc.abstractmethod
    def run(self, input: Any) -> Any:
        """Run this step.

        Parameters
        ----------
        input : object
            Step input.

        Returns
        -------
        object
            Step output.

        """
        raise NotImplementedError

    @property
    def model_version_id(self) -> int:
        """Return the ID of the model version associated with this step."""
        return self._model_version_id

    @property
    def name(self) -> str:
        """Return the name of this step."""
        return self._name

    @property
    def predecessors(self) -> Set[int]:
        """Return the model version IDs of this step's predecessors."""
        return self._predecessors


class ModelContainerStepHandler(_StepHandlerBase):
    """Inference pipeline step handler for an HTTP server model.

    Parameters
    ----------
    name : str
        Name of this step.
    model_version_id : int
        ID of the model version associated with this step.
    predecessors : set of int
        Model version IDs of this steps' predecessors
    prediction_url : str
        REST endpoint for model predictions.

    Attributes
    ----------
    model_version_id
    name
    predecessors

    """

    def __init__(
        self,
        name: str,
        model_version_id: int,
        predecessors: Set[int],
        prediction_url: str,
    ):
        super().__init__(
            name=name,
            model_version_id=model_version_id,
            predecessors=predecessors,
        )
        self._session = http_session.init_session(retry=http_session.retry_config())
        self._prediction_url = prediction_url

    def run(self, input: Any) -> Any:
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
    """Inference pipeline step handler for a locally-instantiated model.

    Parameters
    ----------
    name : str
        Name of this step.
    model_version_id : int
        ID of the model version associated with this step.
    predecessors : set of int
        Model version IDs of this steps' predecessors
    model : object
        Instantiated model ready for predictions.

    Attributes
    ----------
    model_version_id
    name
    predecessors

    """

    def __init__(
        self,
        name: str,
        model_version_id: int,
        predecessors: Set[int],
        model: Any,
    ):
        super().__init__(
            name=name,
            model_version_id=model_version_id,
            predecessors=predecessors,
        )
        self._model = model

    @staticmethod
    def _init_model(
        conn: _utils.Connection,
        model_version_id: int,
    ) -> Any:
        """Return an instantiated model from `model_version_id`.

        Parameters
        ----------
        conn : :class:`~verta._internal_utils._utils.Connection`
            Verta client connection.
        model_version_id : int
            Model version ID.

        Returns
        -------
        object
            Instantiated model ready for predictions.

        """
        model_ver = RegisteredModelVersion._get_by_id(
            conn,
            _utils.Configuration(),
            model_version_id,
        )

        model_cls: Type[Any] = model_ver.get_model()

        model_artifacts: Optional[Dict[str, str]] = None
        model_artifacts_keys: Optional[List[str]] = model_ver.get_attributes().get(
            _MODEL_ARTIFACTS_ATTR_KEY,
        )
        if model_artifacts_keys is not None:
            model_artifacts = model_ver.fetch_artifacts(model_artifacts_keys)

        return model_cls(artifacts=model_artifacts)

    def run(self, input: Any) -> Any:
        return self._model.predict(input)
