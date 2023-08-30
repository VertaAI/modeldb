# -*- coding: utf-8 -*-

from typing import Any, Dict, List, Set, Tuple, Union

from .._internal_utils._utils import Configuration, Connection
from ._pipeline_step import PipelineStep


class PipelineGraph:
    """Object representing a collection of PipelineSteps to be run as a single
    inference pipeline.

    Parameters
    ----------
    steps : list, set, or tuple of :class:`~verta.pipeline.PipelineStep`
        Set of all possible steps of the pipeline. Ordering of steps in the pipeline
        itself is determined by the predecessors provided to each step.

    Attributes
    ----------
    steps: set of :class:`~verta.deployment.PipelineStep`
        Set of PipelineSteps comprising all possible steps in this PiplineGraph.
    """

    def __init__(
        self, steps: Union[List[PipelineStep], Set[PipelineStep], Tuple[PipelineStep]]
    ):
        self._steps = self._validate_steps(steps)

    def __repr__(self) -> str:
        return f"PipelineGraph steps:\n{self._format_steps()}"

    def _format_steps(self) -> str:
        """Format steps for improved readability in __repr__() function."""
        return "\n".join([repr(s) for s in self._steps])

    @property
    def steps(self) -> Set[PipelineStep]:
        return self._steps

    @steps.setter
    def steps(self, value):
        raise AttributeError("can't set attribute 'steps'; please use set_steps()")

    def set_steps(
        self, steps: Union[List[PipelineStep], Set[PipelineStep], Tuple[PipelineStep]]
    ) -> Set[PipelineStep]:
        """Update the set of steps for this PipelineGraph to the provided value.

        Parameters
        ----------
        steps : list, set, tuple of :class:`~verta.deployment.PipelineStep`
            List, set, or tuple of all possible steps of the pipeline graph.
            All options are converted to a set, so order is irrelevant and
            duplicates are removed.

        Returns
        -------
        set of :class:`~verta.deployment.PipelineStep`
            The steps now set for this graph, if validation is successful.

        Raises
        ------
        TypeError
            If ``steps`` is not a set of PipelineStep objects.
        """
        self._steps = set(self._validate_steps(steps))
        return self.steps

    def _validate_steps(
        self, steps: Union[List[PipelineStep], Set[PipelineStep], Tuple[PipelineStep]]
    ) -> Set[PipelineStep]:
        """Validate that the provided steps are a set of  PipelineStep objects.

        Parameters
        ----------
        steps : list, set, or tuple of :class:`~verta.deployment.PipelineStep`
            List, set, or tuple of steps provided by a user.

        Returns
        -------
        set of :class:`~verta.deployment.PipelineStep`
            The same set of steps if validation is successful.

        Raises
        ------
        TypeError
            If steps is not a set of PipelineStep objects.
        """
        if not isinstance(steps, (list, set, tuple)):
            raise TypeError(
                f"steps must be type list, set, or tuple, not {type(steps)}"
            )
        for step in steps:
            if not isinstance(step, PipelineStep):
                raise TypeError(
                    f"individual steps of a PipelineGraph must be type"
                    f" PipelineStep, not {type(step)}."
                )
            # throw an exception if any step's predecessors attr has been inappropriately mutated.
            step._validate_predecessors(step.predecessors)
        if len([s.name for s in steps]) != len(set([s.name for s in steps])):
            raise ValueError("step names must be unique within a PipelineGraph")
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

        This is fed to the backend as 'graph' in our PipelineDefinition schema.
        """
        return [step._to_graph_spec() for step in self.steps]

    def _to_steps_definition(self) -> List[Dict[str, Any]]:
        """Create a pipeline steps specification from this PipelineGraph.

        This is fed to the backend as 'steps' in our PipelineDefinition schema.
        """
        return [step._to_step_spec() for step in self.steps]
