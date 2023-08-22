# -*- coding: utf-8 -*-

from typing import Any, Dict, List

from verta._internal_utils._utils import Configuration, Connection
from ._pipeline_step import PipelineStep


class PipelineGraph:
    """
    A collection of PipelineSteps to be run as a single inference pipeline.

    Parameters
    ----------
    steps : list of :class:`~verta.pipeline.PipelineStep`
        List of all possible steps of the pipeline. Ordering of steps in the pipeline
        itself is determined by the predecessors provided to each step, thus ordering
        of this list is irrelevant.

    Attributes
    ----------
        steps: list of :class:`~verta.deployment.PipelineStep`
            List of PipelineSteps comprising all possible steps in the PiplineGraph.
    """

    def __init__(self, steps: List[PipelineStep]):
        self._steps = self.set_steps(steps)

    def __repr__(self):
        return f"\nPipelineGraph steps:\n{self._format_steps()}"

    def _format_steps(self):
        """Format steps for improved readability in __repr__() function."""
        return "\n".join([str(s) for s in self._steps])

    @property
    def steps(self):
        return self._steps

    @steps.setter
    def steps(self, value):
        raise AttributeError("cannot set attribute 'steps'; please use set_steps()")

    def set_steps(self, steps: List[str]) -> None:
        """
        Set the list of steps for this PipelineGraph.

        Parameters
        ----------
        steps : list of :class:`~verta.deployment.PipelineStep`, optional
            List of all possible steps of the pipline graph. Order does not matter.
        """
        if not isinstance(steps, list):
            raise TypeError("steps must be a list of PipelineStep objects")
        for step in steps:
            if not isinstance(step, PipelineStep):
                raise TypeError("steps must be a list of PipelineStep objects")
        steps = list(set(steps))
        self._steps = steps
        return self.steps

    @classmethod
    def _from_definition(
        cls, pipeline_definition: Dict[str, Any], conn: Connection, conf: Configuration
    ) -> "PipelineGraph":
        """
        Create a PipelineGraph instance from a specification dict.

        Parameters
        ----------
        pipeline_spec : dict
            Specification dict from which to create the Pipeline.
        """
        return cls(
            steps=PipelineStep._steps_from_pipeline_definition(
                pipeline_definition, conn, conf
            ),
        )

    def _to_graph_definition(self) -> List[Dict[str, Any]]:
        """
        Convert this PipelineGraph to a graph dict formatted for a pipeline definition.
        """
        return [step._to_graph_spec() for step in self.steps]

    def _to_steps_definition(self) -> List[Dict[str, Any]]:
        """
        Convert this PipelineGraph to a dict formatted for a pipeline definition.
        """
        return [step._to_step_spec() for step in self.steps]
