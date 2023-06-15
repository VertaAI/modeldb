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

        # DAG nodes are step names
        self._dag: Optional[TopologicalSorter] = None
        # mapping of step names to outputs
        self._outputs: Dict[str, Any] = dict()

    def _prepare_pipeline(self) -> TopologicalSorter:
        """Initialize ``self._dag`` and ``self._outputs``.

        Parameters
        ----------
        pipeline_spec : dict
            Pipeline specification.

        Raises
        ------
        :exc:`~graphlib.CycleError`
            If the pipeline graph has cycles.

        """
        dag = TopologicalSorter(self._pipeline_spec["graph"]["inputs"])
        dag.prepare()
        # TODO: assert one input node
        # TODO: assert one output node

        self._dag = dag
        self._outputs = dict()

    def _run_step(
        self,
        name: str,
        input: Optional[Any] = None,
    ):
        """Run a pipeline step.

        This method stores the step output in ``self._outputs`` and marks it
        as done in ``self._dag``. Given that step names are unique within a
        pipeline, this method is expected to be thread safe.

        Parameters
        ----------
        name : str
            Step name.
        input : object, optional
            Step input. If not provided, output(s) from the step's
            predecessor(s) will be fetched from ``self._outputs``.

        """
        if self._dag is None:
            raise RuntimeError(
                "DAG not initialized; call run() instead of using this method directly",
            )

        step_handler = self._step_handlers[name]
        if input is None:
            predecessors = step_handler.predecessors
            if not predecessors:
                raise ValueError(
                    f"unexpected error: step {name} has no predecessors, but no input was provided",
                )
            if not predecessors <= self._outputs.keys():
                raise RuntimeError(
                    f"unexpected error: step {name}'s predecessors' outputs not found",
                )

            # TODO: figure out how we actually want to collect upstream outputs
            if len(predecessors) == 1:
                input = self._outputs[list(predecessors)[0]]
            else:
                # TODO: figure out how to orchestrate complex pipelines
                raise ValueError("multiple inputs not yet supported")
                # input = {
                #     predecessor: self._outputs[predecessor]
                #     for predecessor in predecessors
                # }

        self._outputs[name] = step_handler.run(input)
        self._dag.done(name)

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
        self._prepare_pipeline()

        # run input node
        # NOTE: assumes only one input node
        step_name: str = self._dag.get_ready()[0]
        self._run_step(step_name, input)

        while self._dag.is_active():
            for step_name in self._dag.get_ready():
                self._run_step(step_name)  # TODO: figure out async

        # return output from final node
        # NOTE: assumes only one leaf node
        return self._outputs[step_name]


class DeployedOrchestrator(_OrchestratorBase):  # TODO
    """Inference pipeline orchestrator using HTTP server models."""


class LocalOrchestrator(_OrchestratorBase):
    """Inference pipeline orchestrator using locally-instantiated models.

    Paremeters
    ----------
    conn : :class:`~verta._internal_utils._utils.Connection`
        Verta client connection.
    pipeline_spec : dict
        Pipeline specification.

    Examples
    --------
    .. code-block:: python

        from verta._pipeline_orchestrator import LocalOrchestrator

        orchestrator = LocalOrchestrator(client._conn, pipeline_spec)
        pipeline_output = orchestrator.run(pipeline_input)

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
