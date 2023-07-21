from typing import Any, Dict, List, Optional

from verta.registry.entities import RegisteredModelVersion
from .._internal_utils._utils import Connection


class PipelineStep:
    """
    A single step within a Pipeline.

    Parameters
    ----------
    model_version : :class:`~verta.registry.entities.RegisteredModelVersion`
        Registered model version to use for the step.
    name: str
        Semantic name of the step, for use in step ordering within a single pipeline.
    input_steps : list, optional
        List of PipelineStep objects whose outputs will be treated as inputs to this step.
        If not included, the step is assumed to be an initial step.
    attributes : dict, optional
        Dictionary of arbitrary attributes to associate with this step. e.g. ``description``
        or ``labels``

    Attributes
    ----------
        model_version
            :class:`~verta.registry.entities.RegisteredModelVersion` run by this step.
        name: str
            Semantic name of the step, for use in step ordering within a single pipeline.
        input_steps : list
            List of PipelineStep objects whose outputs will be treated as inputs to this step.
        attributes: dict
            All configured attributes of this step.
        status: str
            Current status of this step, if part of a deployed pipeline.
    """

    def __init__(
        self,
        model_version: RegisteredModelVersion,
        name: str,
        input_steps: Optional[
            List["PipelineStep"]
        ] = None,  # Could be first step with no upstream inputs
        attributes: Optional[Dict[str, Any]] = None,
    ):
        self.model_version = model_version
        self.name = name
        self.input_steps = input_steps
        self.attributes = attributes
        self._status = None

    #TODO add properties for mod version, name, input steps, attributes, status

    def __repr__(self):
        return "\n".join(
            (
                f"step name: {self.name}",
                f"registered_model_id: {self.model_version.registered_model_id}",
                f"registered_model_version_name: {self.model_version.name}",
                f"model_version_id: {self.model_version.id}",
                f"input_steps: {self.input_steps}",
                f"attributes: {self.attributes}",
            )
        )

    @classmethod
    def _from_pipeline_spec(
        cls, step_name: str, pipeline_spec: Dict[str, Any], conn: Connection
    ) -> "PipelineStep":
        """
        Return a PipelineStep object by name from a pipeline specification

        Parameters
        ----------
        step_name : str
            User-defined, semantic name of the step to return
        pipeline_spec : dict
            Specification dictionary for the whole pipeline
        conn : :class:`~verta._internal_utils._utils.Connection`
            Connection object for fetching the model version associated with the step

        Returns
        -------
        :class:`~verta._pipelines.PipelineStep`
            PipelineStep object corresponding to the step name

        Raises
        ------
        ValueError
            If the step name is not found in the pipeline specification.
        """
        steps = [s["name"] for s in pipeline_spec["steps"]]
        if step_name not in steps:
            raise ValueError(
                f"step with name {step_name} not found in pipeline specification"
            )
        step = [s for s in pipeline_spec["steps"] if s["name"] == step_name][0]
        inputs = [
            i["inputs"] for i in pipeline_spec["graph"] if i["name"] == step_name
        ][0]
        attrs = {a["key"]: a["value"] for a in step["attributes"]}
        registered_model = RegisteredModelVersion._get_proto_by_id(
            conn, step["model_version_id"]
        )
        return cls(
            model_version=registered_model,
            name=step_name,
            input_steps=inputs,
            attributes=attrs,
        )

    def _to_steps_spec(self) -> Dict[str, Any]:
        """
        Return a specification dictionary from a PipelineStep object

        Returns
        -------
        dict representing the step, formatted for the pipeline specification
        """
        attrs = [{"key": k, "value": v} for k, v in self.attributes.items()]
        return {
            "name": self.name,
            "model_version_id": self.model_version.id,
            "attributes": attrs,
        }

    def _to_graph_spec(self) -> Dict[str, Any]:
        """
        Return an input graph dictionary from a PipelineStep object

        Returns
        -------
        dict representing the step's inputs, formatted for the graph in the pipeline
        specification
        """
        return {
            "name": self.name,
            "inputs": [s for s in self.input_steps],
        }

    def add_input_steps(self, steps: List[str]):
        """
        Add the provided input steps, using their ``name`` attribute, to this step.

        Parameters
        ----------
        steps : list of str
            List of the names corresponding to pipeline steps whose outputs will be treated
            as inputs to this step.
        """
        self.input_steps.extend(steps)

    def remove_input_steps(self, steps: List[str]):
        """
        Remove the provided input steps, using their ``name`` attribute, from this step.

        Parameters
        ----------
        steps : list of str
            List of the names corresponding to pipeline steps to remove as inputs to this
            step.
        """
        for step in steps:
            if step not in self.input_steps:
                raise ValueError(
                    f"step {step} not found in input steps for step {self.name}"
                )
            self.input_steps.remove(step)

    def change_model_version(self, model_version: RegisteredModelVersion):
        """
        Change the model version associated with this step.

        Parameters
        ----------
        model_version : :class:`~verta.registry.entities.RegisteredModelVersion`
            Registered model version to use for the step.
        """
        if not isinstance(model_version, RegisteredModelVersion):
            raise ValueError(
                f"model_version must be a RegisteredModelVersion object, not {type(model_version)}"
            )
        self.model_version = model_version
