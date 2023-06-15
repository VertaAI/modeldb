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
        step_handlers: Dict[str, _StepHandlerBase],
    ):
        self._pipeline_spec = pipeline_spec
        self._step_handlers = step_handlers
        self._dag: Optional[TopologicalSorter] = None

    def _run_step(
        self,
        name: str,
        input: Any,
    ) -> Any:
        """Run a pipeline step.

        Parameters
        ----------
        name : str
            Step name.
        input : object
            Step input.

        Returns
        -------
        object
            Step output.

        """
        if self._dag is None:
            raise RuntimeError("DAG not initialized")

        step_handler = self._step_handlers[name]

        output = step_handler.run(input)

        self._dag.done(name)
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
        :class:`~graphlib.TopologicalSorter`
            Pipeline graph. Each node is a step name.

        Raises
        ------
        :exc:`~graphlib.CycleError`
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
        step_name: str = self._dag.get_ready()[0]
        outputs = {
            step_name: self._run_step(step_name, input),
        }

        while self._dag.is_active():
            for step_name in self._dag.get_ready():
                input = None

                self._run_step(step_name, input)

                step_handler = self._step_handlers[step_name]
                outputs[step_name] = step_handler.run(  # TODO: async?
                    outputs[step_handler.predecessors[0]]  # just to test
                    # {
                    #     predecessor_id: outputs[predecessor_id]
                    #     for predecessor_id in step_handler.predecessors
                    # }
                )

        # return output from final node
        # NOTE: assumes only one leaf node
        return outputs[step_handler.name]


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
    ) -> Dict[str, ModelObjectStepHandler]:
        """Instantiate and return step handlers.

        Parameters
        ----------
        conn : :class:`~verta._internal_utils._utils.Connection`
            Verta client connection.
        pipeline_spec : dict
            Pipeline specification.

        Returns
        -------
        dict of str to :class:`~verta._pipeline_orchestrator._step_handler.ModelObjectStepHandler`
            Mapping of step names to their handlers.

        """
        step_handlers = dict()
        for step in pipeline_spec["steps"]:
            step_name = step["attributes"]["name"]
            step_handlers[step_name] = ModelObjectStepHandler(
                name=step_name,
                predecessors=pipeline_spec["graph"]["inputs"].get(step_name, []),
                model=ModelObjectStepHandler._init_model(conn, step["rmvId"]),
            )
        return step_handlers
