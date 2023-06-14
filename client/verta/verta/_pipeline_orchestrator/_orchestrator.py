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
        dag = self.get_dag()

        # run input node
        # NOTE: assumes only one input node
        model_version_id = dag.get_ready()[0]
        step_handler = self._step_handlers[model_version_id]
        outputs = {model_version_id: step_handler.run(input)}
        dag.done(model_version_id)

        while dag.is_active():
            for model_version_id in dag.get_ready():
                step_handler = self._step_handlers[model_version_id]
                outputs[model_version_id] = step_handler.run(  # TODO: async?
                    outputs[step_handler.predecessors[0]]  # TEST
                    # {
                    #     predecessor_id: outputs[predecessor_id]
                    #     for predecessor_id in step_handler.predecessors
                    # }
                )
                dag.done(model_version_id)  # TODO: callback?

        # return output from final node
        # NOTE: assumes only one leaf node
        return outputs[step_handler.model_version_id]


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
