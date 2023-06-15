# -*- coding: utf-8 -*-

import abc
from typing import Any, Dict, Optional

from verta._internal_utils import _utils
from verta.external.cpython.graphlib import TopologicalSorter

from ._step_handler import (
    _StepHandlerBase,
    ModelObjectStepHandler,
)


class _OrchestratorBase(abc.ABC):
    """Abstract base class for inference pipeline orchestrators."""

    def __init__(
        self,
        pipeline_spec: Dict[str, Any],
        step_handlers: Dict[int, _StepHandlerBase],
    ):
        self._pipeline_spec = pipeline_spec
        self._step_handlers = step_handlers
        self._dag: Optional[TopologicalSorter] = None

    def _run_step(
        self,
        model_version_id: int,
        input: Any,
    ) -> Any:
        """Run a pipeline step.

        Parameters
        ----------
        model_version_id : int
            ID of the step's model version.
        input : object
            Step input.

        Returns
        -------
        object
            Step output.

        """
        if self._dag is None:
            raise RuntimeError("DAG not initialized")

        step_handler = self._step_handlers[model_version_id]


        output = step_handler.run(input)


        self._dag.done(model_version_id)
        return output

    @staticmethod
    def _init_dag(
        pipeline_spec: Dict[str, Any],
    ) -> TopologicalSorter:
        """Validate and return a pipeline graph.

        Parameters
        ----------
        pipeline_spec : dict
            Pipeline specification.

        Returns
        -------
        graphlib.TopologicalSorter
            Pipeline graph. Each node is a model version ID.

        Raises
        ------
        graphlib.CycleError
            If the pipeline graph has cycles.

        """
        dag = TopologicalSorter(pipeline_spec["graph"]["inputs"])
        dag.prepare()
        # TODO: assert one input node
        # TODO: assert one output node
        return dag

    def run(
        self,
        input: Any,
    ) -> Any:
        """Run this orchestrator's pipeline.

        Parameters
        ----------
        input : object
            Input for the pipeline's input node.

        Return
        ------
        object
            Output from the pipeline's output node.

        """
        self._dag = self._init_dag(self._pipeline_spec)

        # run input node
        # NOTE: assumes only one input node
        model_version_id: int = self._dag.get_ready()[0]
        outputs = {
            model_version_id: self._run_step(model_version_id, input),
        }

        while self._dag.is_active():
            for model_version_id in self._dag.get_ready():
                input =

                self._run_step(
                    model_version_id,
                )

                step_handler = self._step_handlers[model_version_id]
                outputs[model_version_id] = step_handler.run(  # TODO: async?
                    outputs[step_handler.predecessors[0]]  # just to test
                    # {
                    #     predecessor_id: outputs[predecessor_id]
                    #     for predecessor_id in step_handler.predecessors
                    # }
                )

        # return output from final node
        # NOTE: assumes only one leaf node
        return outputs[step_handler.model_version_id]


class DeployedOrchestrator(_OrchestratorBase):
    """Inference pipeline orchestrator using HTTP server models."""

    def __init__(
        self,
        pipeline_spec: Dict[str, Any],
    ):
        raise NotImplementedError


class LocalOrchestrator(_OrchestratorBase):
    """Inference pipeline orchestrator using locally-instantiated models.

    Paremeters
    ----------
    conn : :class:`~verta._internal_utils._utils.Connection`
        Verta client connection.
    pipeline_spec : dict
        Pipeline specification.

    """

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
        """Instantiate and return step handlers.

        Parameters
        ----------
        conn : :class:`~verta._internal_utils._utils.Connection`
            Verta client connection.
        pipeline_spec : dict
            Pipeline specification.

        Returns
        -------
        dict of int to :class:`~verta._pipeline_orchestrator._step_handler.ModelObjectStepHandler`
            Mapping of model version IDs to their step handlers.

        """
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
