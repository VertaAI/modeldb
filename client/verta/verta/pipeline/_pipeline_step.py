# -*- coding: utf-8 -*-

from typing import Any, Dict, List, Optional, Set

from verta._internal_utils._utils import Configuration, Connection
from verta.registry.entities import RegisteredModel, RegisteredModelVersion


class PipelineStep:
    """A single step within an inference pipeline, representing a single model
    version to be run.

    Parameters
    ----------
    name : str
        Name of the step, for use within the scope of the pipeline only.
    model_version : :class:`~verta.registry.entities.RegisteredModelVersion`
        Registered model version to run for the step.
    predecessors : set, optional
        Set of PipelineSteps whose outputs will be treated as inputs to this step.
        If not included, the step is assumed to be an initial step. Values must be unique.

    Attributes
    ----------
    name : str
        Name of the step within the scope of the pipeline.
    model_version : :class:`~verta.registry.entities.RegisteredModelVersion`
        Model version being run by this step.
    predecessors : list
        List of PipelineSteps whose outputs will be treated as inputs to this step.
    """

    def __init__(
        self,
        name: str,
        model_version: RegisteredModelVersion,
        predecessors: Optional[
            Set["PipelineStep"]
        ] = None,  # Optional because it could be the first step with no predecessors
    ):
        self._name = self.set_name(name)
        self._model_version = self.set_model_version(model_version)
        self._predecessors = (
            self._validate_predecessors(predecessors) if predecessors else set()
        )
        self._registered_model_id = self._model_version.registered_model_id
        self._registered_model: RegisteredModel = self._get_registered_model(
            conn=model_version._conn, conf=model_version._conf
        )

    def __repr__(self) -> str:
        return "\n    ".join(
            (
                "\n    PipelineStep:",
                f"step name: {self.name}",
                f"registered_model: {self._registered_model.name}",
                f"registered_model_id: {self._registered_model_id}",
                f"registered_model_version: {self.model_version.name}",
                f"registered_model_version_id: {self.model_version.id}",
                f"predecessors: {[s.name for s in self.predecessors]}",
            )
        )

    @property
    def model_version(self) -> RegisteredModelVersion:
        return self._model_version

    @model_version.setter
    def model_version(self, value) -> None:
        """Raise a more informative error than the default."""
        raise AttributeError(
            "can't set attribute 'model_version'; please use set_model_version()"
        )

    def set_model_version(self, new_model_version: RegisteredModelVersion) -> None:
        """Change the registered model version associated with this step.

        Parameters
        ----------
        model_version : :class:`~verta.registry.entities.RegisteredModelVersion`
            Registered model version to use for the step.
        """
        if not isinstance(new_model_version, RegisteredModelVersion):
            raise TypeError(
                f"model_version must be a RegisteredModelVersion object, not {type(new_model_version)}"
            )
        self._model_version = new_model_version
        return self.model_version

    @property
    def name(self) -> str:
        return self._name

    @name.setter
    def name(self, value) -> None:
        """Raise a more informative error than the default."""
        raise AttributeError("can't set attribute 'name'; please use set_name()")

    def set_name(self, name: str) -> str:
        """Change the name of this step.

        Parameters
        ----------
        name : str
            New name to use for the step.
        """
        if not isinstance(name, str):
            raise TypeError(f"name must be a string, not {type(name)}")
        self._name = name
        return self.name

    @property
    def predecessors(self) -> Set["PipelineStep"]:
        return self._validate_predecessors(self._predecessors)

    @predecessors.setter
    def predecessors(self, value) -> None:
        """Raise a more informative error than the default."""
        raise AttributeError(
            "can't set attribute 'predecessors'; please use set_predecessors()"
        )

    def set_predecessors(self, steps: Set["PipelineStep"]) -> set:
        """Set the predecessors associated with this step.

        Parameters
        ----------
        steps : list
            List of PipelineStep objects whose outputs will be treated as inputs to this step.
        """
        self._predecessors = self._validate_predecessors(steps)
        return self.predecessors

    def _validate_predecessors(
        self, predecessors: Set["PipelineStep"]
    ) -> set["PipelineStep"]:
        """Validate that the provided predecessors are a set of PipelineStep objects."""
        if not isinstance(predecessors, set):
            raise TypeError(f"steps must be type set, not {type(predecessors)}")
        for step in predecessors:
            if not isinstance(step, PipelineStep):
                raise TypeError(
                    f"individual predecessors of a PipelineStep must be type"
                    f" PipelineStep, not {type(step)} for predecessor '{step}'"
                )
        return predecessors

    def _get_registered_model(self, conn: Connection, conf: Configuration) -> None:
        """Fetch the registered model associated with this step's model version.

        This is to provide important context to the user via the _repr_ method
        when a registered pipeline is fetched from the backend.
        """
        rm = RegisteredModel._get_by_id(
            id=self._registered_model_id, conn=conn, conf=conf
        )
        self._registered_model = rm
        return rm

    @classmethod
    def _steps_from_pipeline_definition(
        cls, pipeline_definition: Dict[str, Any], conn: Connection, conf: Configuration
    ) -> Set["PipelineStep"]:
        """Return a list of PipelineStep objects from a pipeline definition.

        This method is used when fetching a pre-existing pipeline from the backend
        and converting it to a local RegisteredPipeline object, which includes the
        PipelineGraph and all component steps as PipelineStep objects.

        Parameters
        ----------
        pipeline_definition : dict
            Specification dictionary for the whole pipeline
        conn : :class:`~verta._internal_utils._utils.Connection`
            Connection object for fetching the model version associated with the step
        conf: :class:`~verta._internal_utils._utils.Configuration`
            Configuration object for fetching the model version associated with the step

        Returns
        -------
        list of :class:`~verta._pipelines.PipelineStep`
            List of steps in the pipeline spec as PipelineStep objects
        """
        steps: Set["PipelineStep"] = set()
        for step in pipeline_definition["steps"]:
            steps.add(
                cls(
                    name=step["name"],
                    model_version=RegisteredModelVersion._get_by_id(
                        id=step["model_version_id"], conn=conn, conf=conf
                    ),
                    predecessors=set(),
                )
            )
        for step_object in steps:
            predecessor_names = [
                s["predecessors"]
                for s in pipeline_definition["graph"]
                if s["name"] == step_object.name
            ][0]
            step_object.set_predecessors(
                {s for s in steps if s.name in predecessor_names}
            )
        return steps

    def _to_step_spec(self) -> Dict[str, Any]:
        """Return a dictionary representation of this step, formatted for a
        pipeline definition.
        """
        return {
            "name": self.name,
            "model_version_id": self.model_version.id,
        }

    def _to_graph_spec(self) -> Dict[str, Any]:
        """Return a dictionary representation of predecessors for this step,
        formatted for a pipeline definition.
        """
        return {
            "name": self.name,
            "predecessors": [s.name for s in self.predecessors],
        }
