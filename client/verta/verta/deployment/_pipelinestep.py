from typing import Any, Dict, List, Optional

from verta.registry.entities import RegisteredModelVersion


class PipelineStep:
    """
    A single step within a Pipeline.

    Parameters
    ----------
    model_version : :class:`~verta.registry.entities.RegisteredModelVersion`
        Registered model version to use for the step.
    name: str
        Semantic name of the step, for use in step ordering within a single pipeline.
    input_steps : list of PipelineStep objects, optional
        List of PipelineSteps whose outputs will be treated as inputs to this step.
        If not included, the step is assumed to be an initial step.
    attributes : dict, optional
        Dictionary of arbitrary attributes to associate with this step. e.g. ``description``
        or ``labels``

    Attributes
    ----------
        model_version
            :class:`~verta.registry.entities.RegisteredModelVersion` run by this step.
        attributes: dict
            All configured attributes of this step.
    """

    def __init__(
        self,
        model_version: RegisteredModelVersion,
        input_steps: Optional[
            List["PipelineStep"]
        ] = None,  # Could be first step with no upstream inputs
        attributes: Optional[Dict[str, Any]] = None,
    ):
        self.id = id
        self.model_version = model_version
        self.input_steps = input_steps
        self.output = list()


    def __repr__(self):
        pass

    @classmethod
    def get_step(self, name: str = None, id: int = None) -> "PipelineStep":
        """Get the step with the given name or ID from the registry"""
        # Here would be the HTTP request to backend for a specific step
        # return cls(response.json())
        pass

    @classmethod
    def get_steps(self) -> List["PipelineStep"]:
        """Get the step with the given name or ID from the registry"""
        # Here would be the HTTP request to backend for a list of steps
        # return [cls(step) for step in response.json()]
        pass
