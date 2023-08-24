# -*- coding: utf-8 -*-

from typing import Any, Dict, List, Set

from verta._internal_utils._utils import Configuration, Connection
from ._pipeline_step import PipelineStep


class PipelineGraph:
    """Object representing a collection of PipelineSteps to be run as a single
    inference pipeline.

    Parameters
    ----------
    steps : set of :class:`~verta.pipeline.PipelineStep`
        Set of all possible steps of the pipeline. Ordering of steps in the pipeline
        itself is determined by the predecessors provided to each step.

    Attributes
    ----------
    steps: set of :class:`~verta.deployment.PipelineStep`
        Set of PipelineSteps comprising all possible steps in this PiplineGraph.
    """

    def __init__(self, steps: Set[PipelineStep]):
        self._steps = self._validate_steps(steps)
        self._predecessors = [s.predecessors for s in self._steps]
        # throws an error if any step's predecessors attr has been inappropriately mutated.

    def __repr__(self) -> str:
        return f"\nPipelineGraph steps:\n{self._format_steps()}"

    def _format_steps(self) -> str:
        """Format steps for improved readability in __repr__() function."""
        return "\n".join([repr(s) for s in self._steps])

    @property
    def steps(self) -> Set[PipelineStep]:
        return self._validate_steps(self._steps)

    @steps.setter
    def steps(self, value):
        raise AttributeError("can't set attribute 'steps'; please use set_steps()")

    def set_steps(self, steps: Set[PipelineStep]) -> Set[PipelineStep]:
        """Update the set of steps for this PipelineGraph to the provided value.

        Parameters
        ----------
        steps : set of :class:`~verta.deployment.PipelineStep`
            Set of all possible steps of the pipline graph.
        """
        self._steps = self._validate_steps(steps)
        return self.steps

    def _validate_steps(self, steps: Set[PipelineStep]) -> Set[PipelineStep]:
        """Validate that the provided steps are a set of  PipelineStep objects.

        Parameters
        ----------
        steps : set of :class:`~verta.deployment.PipelineStep`
            Set of steps provided by a user.

        Returns
        -------
        set of :class:`~verta.deployment.PipelineStep`
            The same set of steps if validation is successful.

        Raises
        ------
        TypeError
            If steps is not a set of PipelineStep objects.
        """
        if not isinstance(steps, set):
            raise TypeError(f"steps must be type set, not {type(steps)}")
        for step in steps:
            if not isinstance(step, PipelineStep):
                raise TypeError(
                    f"individual steps of a PipelineGraph must be type"
                    f" PipelineStep, not {type(step)}"
                )
        return steps

    @classmethod
    def _from_definition(
        cls, pipeline_definition: Dict[str, Any], conn: Connection, conf: Configuration
    ) -> "PipelineGraph":
        """Create a PipelineGraph instance from a specification dict.

        This is used to return a PipelineGraph object when fetching an existing registered
        pipeline from the backend in the form of a dict extracted from a `pipeline.json`
        artifact.

        Parameters
        ----------
        pipeline_definition : dict
            Pipeline definition dict from which to create the Pipeline.
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
        to remain json serializable, as this will be converted and uploaded as an artifact.
        """
        return [step._to_graph_spec() for step in self.steps]

    def _to_steps_definition(self) -> List[Dict[str, Any]]:
        """Create a pipeline steps specification from this PipelineGraph.

        The back-end expects a list of steps and their model versions as part of the
        `steps` object within a PipelineDefinition. This method converts this PipelineGraph
        to a formatted list of steps with model versions for that purpose. A list is used
        to remain json serializable, as this will be converted and uploaded as an artifact.
        """
        return [step._to_step_spec() for step in self.steps]
