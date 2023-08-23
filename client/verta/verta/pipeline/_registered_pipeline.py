# -*- coding: utf-8 -*-

import json
import copy
from typing import Any, Dict, Optional

import tempfile

from verta._internal_utils._utils import Configuration, Connection
from verta.endpoint.resources import Resources
from verta.pipeline import PipelineGraph
from verta.registry.entities import RegisteredModelVersion


class RegisteredPipeline:
    """Object representing a version of a registered inference pipeline.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.create_registered_pipeline() <verta.Client.create_registered_piepline>`
    for creating a new pipeline, or
    :meth:`Client.get-registered_pipeline() <verta.Client.get_registered_piepline>`
    for fetching existing pipelines.

    Attributes
    ----------
    name: str
        Name of this pipeline.
    id: int
        Auto-assigned ID of this Pipeline.
    graph: :class:`~verta.pipeline.PipelineGraph`
        PipelineGraph object containing all possible steps in the Pipline.
    """

    def __init__(
        self,
        registered_model_version: RegisteredModelVersion,
        graph: PipelineGraph,
    ):
        """Create a Pipeline instance from an existing RegisteredModelVersion object
        and the provided pipeline graph.

        Name and ID are captured once upon creation to avoid additional HTTP calls
        to refresh the cache of the RMV, because pipelines are immutable.
        """
        self._registered_model_version = registered_model_version
        self._name = self._registered_model_version.name
        self._id = self._registered_model_version.id
        self._graph = graph

    def __repr__(self):
        return "\n".join(
            (
                "RegisteredPipeline:",
                f"pipeline name: {self.name}",
                f"pipeline id: {self.id}",
                f"\n{self._graph}",
            )
        )

    @property
    def name(self):
        return self._name

    @property
    def id(self):
        return self._id

    @property
    def graph(self):
        return self._graph

    def copy_graph(self) -> PipelineGraph:
        """Return a deep copy of the PipelineGraph of this pipeline.

        RegisteredPipeline objects are immutable once registered with Verta. This
        function returns a PipelineGraph object that can be modified and used to
        create and register a new RegisteredPipeline.
        """
        return copy.deepcopy(self._graph)

    def _log_pipeline_definition_artifact(self) -> None:
        """
        Log the pipeline definition as an artifact of the registered model version.
        """
        with tempfile.NamedTemporaryFile() as temp_file:
            bytes = json.dumps(self._to_pipeline_definition()).encode("utf-8")
            temp_file.write(bytes)
            self._registered_model_version.log_artifact("pipeline.json", temp_file)

    def _to_pipeline_definition(self) -> Dict[str, Any]:
        """Create a complete pipeline definition dict from a name and PipelineGraph.

        Used in conjunction with the client function for creating a registered
        pipeline from a pipeline graph.
        """
        return {
            "pipeline_version_id": self.id,
            "graph": self._graph._to_graph_definition(),
            "predecessors": self._graph._to_steps_definition(),
        }

    def _to_pipeline_configuration(
        self, pipeline_resources: Optional[Dict[str, Resources]] = None
    ) -> Dict[str, Any]:
        """Build a pipeline configuration dict for this pipeline.

        The `env` and `build` keys are not included in the configuration
        resulting in default values being used by the backend.

        Parameters
        ----------
        pipeline_resources : dict of str to :class:`~verta.endpoint.resources.Resources`, optional
            Resources to be allocated to each step of the pipeline. Keys are step names.

        Returns
        -------
        dict
            Representation of a pipeline configuration.
        """
        if pipeline_resources:
            for res in pipeline_resources.values():
                if not isinstance(res, Resources):
                    raise TypeError(
                        f"pipeline_resources values must be type Resources, not {type(res)}"
                    )
            for step_name in pipeline_resources.keys():
                if not isinstance(step_name, str):
                    raise TypeError(
                        f"pipeline_resources keys must be type str, not {type(step_name)}"
                    )
                if step_name not in [step.name for step in self._graph.steps]:
                    raise ValueError(
                        f"pipeline_resources contains resources for a step not in "
                        f"the pipeline: '{step_name}'"
                    )
        steps = list()
        for step in self._graph.steps:
            step_config = {
                "name": step.name,
            }
            if pipeline_resources:
                step_res = pipeline_resources.get(step.name, None)
                if step_res:
                    step_config["resources"] = step_res._as_dict()
            steps.append(step_config)
        return {
            "pipeline_version_id": self.id,
            "steps": steps,
        }

    @classmethod
    def _get_pipeline_definition_artifact(
        cls, registered_model_version: RegisteredModelVersion
    ) -> Dict[str, Any]:
        """Get the pipeline definition artifact from the registered model version.

        Parameters
        ----------
        registered_model_version : :class:`~verta.registry.entities.RegisteredModelVersion`
            RegisteredModelVersion object associated with this pipeline, from which
            the pipeline definition artifact will be fetched.

        Returns
        -------
        dict
            Pipeline definition dictionary.
        """
        definition = registered_model_version.get_artifact("pipeline.json").read()
        return json.loads(definition.decode("utf-8"))

    @classmethod
    def _from_pipeline_definition(
        cls,
        registered_model_version: RegisteredModelVersion,
    ) -> "RegisteredPipeline":
        """Create a Pipeline instance from a specification dict.

        Used when fetching a registered pipeline from the Verta backend.

        Parameters
        ----------
        registered_model_version : :class:`~verta.registry.entities.RegisteredModelVersion`
            RegisteredModelVersion object associated with this pipeline.
        pipeline_definition : dict
            Specification dict from which to create the Pipeline.
        """
        pipeline_definition = cls._get_pipeline_definition_artifact(
            registered_model_version
        )
        return cls(
            registered_model_version=registered_model_version,
            graph=PipelineGraph._from_definition(
                pipeline_definition=pipeline_definition,
                conn=registered_model_version._conn,
                conf=registered_model_version._conf,
            ),
        )
