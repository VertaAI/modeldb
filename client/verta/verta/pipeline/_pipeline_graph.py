# -*- coding: utf-8 -*-

from typing import Any, Dict, List, Set

from verta._internal_utils._utils import Configuration, Connection
from ._pipeline_step import PipelineStep


class PipelineGraph:
    """A collection of PipelineSteps to be run as a single inference pipeline.

    Parameters
    ----------
    steps : set of :class:`~verta.pipeline.PipelineStep`
        List of all possible steps of the pipeline. Ordering of steps in the pipeline
        itself is determined by the predecessors provided to each step, thus ordering
        of this list is irrelevant.

    Attributes
    ----------
    steps: set of :class:`~verta.deployment.PipelineStep`
        Set of PipelineSteps comprising all possible steps in the PiplineGraph.
    """

    def __init__(self, steps: Set[PipelineStep]):
        self._steps = self.set_steps(steps)

    def __repr__(self):
        return f"\nPipelineGraph steps:\n{self._format_steps()}"

    def _format_steps(self):
        """Format steps for improved readability in __repr__() function."""
        return "\n".join([repr(s) for s in self._steps])

    @property
    def steps(self):
        return self._steps

    @steps.setter
    def steps(self, value):
        raise AttributeError("cannot set attribute 'steps'; please use set_steps()")

    def set_steps(self, steps: Set[PipelineStep]) -> Set[PipelineStep]:
        """Update the set of steps for this PipelineGraph to the provided value.

        Parameters
        ----------
        steps : set of :class:`~verta.deployment.PipelineStep`
            Set of all possible steps of the pipline graph. Order does not matter.
        """
        if not isinstance(steps, set):
            raise TypeError(f"steps must be type set, not {type(steps)}")
        for step in steps:
            if not isinstance(step, PipelineStep):
                raise TypeError(f"individual steps must be type PipelineStep, not {type(step)}")
        self._steps = steps
        return self.steps

    @classmethod
    def _from_definition(
        cls, pipeline_definition: Dict[str, Any], conn: Connection, conf: Configuration
    ) -> "PipelineGraph":
        """Create a PipelineGraph instance from a specification dict.

        Parameters
        ----------
        pipeline_spec : dict
            Specification dict from which to create the Pipeline.
        conn : :class:`~verta._internal_utils._utils.Connection`
            Connection object for fetching the model version associated with the step
        conf: :class:`~verta._internal_utils._utils.Configuration`
            Configuration object for fetching the model version associated with the step
        """
        return cls(
            steps=PipelineStep._steps_from_pipeline_definition(
                pipeline_definition, conn, conf
            ),
        )

    def _to_graph_definition(self) -> List[Dict[str, Any]]:
        """Create a pipeline graph specification from this PipelineGraph.

        The back-end expects a list of steps and their predecessors as part of the
        `graph` object within a PipelineDefinition. This method converts this PipelineGraph
        to a formatted list of steps with predecessors for that purpose. A list is used
        to remain json serializable.
        """
        return [step._to_graph_spec() for step in self.steps]

    def _to_steps_definition(self) -> List[Dict[str, Any]]:
        """Create a pipeline steps specification from this PipelineGraph.

        The back-end expects a list of steps and their model versions as part of the
        `steps` object within a PipelineDefinition. This method converts this PipelineGraph
        to a formatted list of steps with model versions for that purpose. A list is used
        to remain json serializable.
        """
        return [step._to_step_spec() for step in self.steps]
