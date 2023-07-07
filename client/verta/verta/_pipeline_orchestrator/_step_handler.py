# -*- coding: utf-8 -*-

import abc
from typing import Any, Dict, Iterable, List, Optional, Set, Type

from verta._internal_utils import _utils
from verta.registry.entities import RegisteredModelVersion
from verta.tracking.entities._entity import _MODEL_ARTIFACTS_ATTR_KEY


class _StepHandlerBase(abc.ABC):
    """Abstract base class for inference pipeline step handlers."""

    def __init__(
        self,
        name: str,
        predecessors: Iterable[str],
    ):
        self._name = name
        self._predecessors = set(predecessors)

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
    def name(self) -> str:
        """Return the name of this step."""
        return self._name

    @property
    def predecessors(self) -> Set[str]:
        """Return the names of this step's predecessors."""
        return self._predecessors


class ModelObjectStepHandler(_StepHandlerBase):
    """Inference pipeline step handler for a locally-instantiated model.

    Parameters
    ----------
    name : str
        Name of this step.
    predecessors : iterable of str
        Names of this steps' predecessors
    model : object
        Instantiated model ready for predictions.

    Attributes
    ----------
    name
    predecessors

    """

    def __init__(
        self,
        name: str,
        predecessors: Iterable[str],
        model: Any,
    ):
        super().__init__(
            name=name,
            predecessors=predecessors,
        )
        self._model = model

    @staticmethod
    def _init_model(
        conn: _utils.Connection,
        model_version_id: int,
    ) -> Any:
        """Instantiate and return the model from `model_version_id`.

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
