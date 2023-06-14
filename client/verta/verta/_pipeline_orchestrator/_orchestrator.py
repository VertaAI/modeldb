# -*- coding: utf-8 -*-

import abc
from typing import Any, Dict

from verta._internal_utils import _utils
from verta.external.cpython.graphlib import TopologicalSorter

from ._step_handler import (
    _StepHandlerBase,
    ModelObjectStepHandler,
)


class _OrchestratorBase(abc.ABC):
    def __init__(
        self,
        pipeline_spec: Dict[str, Any],
        step_handlers: Dict[int, _StepHandlerBase],
    ):
        self._pipeline_spec = pipeline_spec
        self._step_handlers = step_handlers

    def get_dag(self) -> TopologicalSorter:
        dag = TopologicalSorter(self._pipeline_spec["graph"]["inputs"])
        dag.prepare()
        # TODO: assert one input node
        return dag

    def run(
        self,
        input: Any,
    ):
        raise NotImplementedError
        dag = self.get_dag()
        while dag.is_active():
            for model_version_id in dag.get_ready():
                step_handler = self._step_handlers[model_version_id]
                output = step_handler.run(input)  # TODO: async?
                # TODO: track outputs
                dag.done(model_version_id)  # TODO: callback?


class DeployedOrchestrator(_OrchestratorBase):
    def __init__(
        self,
        pipeline_spec: Dict[str, Any],
    ):
        raise NotImplementedError
        super().__init__(
            pipeline_spec=pipeline_spec,
        )


class LocalOrchestrator(_OrchestratorBase):
    def __init__(
        self,
        conn: _utils.Connection,
        pipeline_spec: Dict[str, Any],
    ):
        super().__init__(
            pipeline_spec=pipeline_spec,
            step_handlers=self._init_step_handlers(conn, pipeline_spec),
        )

    @staticmethod
    def _init_step_handlers(
        conn: _utils.Connection,
        pipeline_spec: Dict[str, Any],
    ) -> Dict[int, ModelObjectStepHandler]:
        step_handlers = dict()
        for step in pipeline_spec["steps"]:
            model_version_id = step["rmvId"]
            step_handlers[model_version_id] = ModelObjectStepHandler(
                name=step["attributes"]["name"],
                model_version_id=model_version_id,
                predecessors=pipeline_spec["graph"]["inputs"].get(model_version_id, []),
                model=ModelObjectStepHandler._init_model(conn, model_version_id),
            )
        return step_handlers
