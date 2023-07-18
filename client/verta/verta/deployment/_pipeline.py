from typing import Any, Dict


class Pipeline:
    """
    A collection of PipelineSteps to be run as part of a single inference pipeline

    Parameters
    ----------
    name : str
        Name of the pipeline.
    steps : list of :class:`~verta.deployment.PipelineStep`
        All possible steps of the Pipline.

    Attributes
    ----------
        id
            ID of this Pipeline in the registry, if registered.
        name: str
            Name of this pipeline.
        steps: list of :class:`~verta.deployment.PipelineStep`
            List of PipelineSteps comprising all possible steps in the Pipline.
        status: dict
            Current status of this Pipeline, including status of all component steps.
    """

    def __init__(self, name: str, steps, description):
        self.name = name
        self.steps = steps
        self.description = description
        self.id = None

    def add_steps(self, steps):
        pass

    def remove_steps(self, steps):
        pass

    def from_spec(self, pipeline_spec: Dict[str, Any]):
        pass
