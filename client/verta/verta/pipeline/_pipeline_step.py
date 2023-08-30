# -*- coding: utf-8 -*-

from typing import Any, Dict, List, Optional, Set, Tuple, Union

from .._internal_utils._utils import Configuration, Connection
from ..registry.entities import RegisteredModel, RegisteredModelVersion


class PipelineStep:
    """Object representing a single step to be run within an inference pipeline.

    Parameters
    ----------
    name : str
        Name of the step, for use within the scope of the pipeline only.
    registered_model_version : :class:`~verta.registry.entities.RegisteredModelVersion`
        Registered model version to run for this step.
    predecessors : list, set, tuple, optional
        List, set, or tuple of unique PipelineStep objects whose outputs will be treated as
        inputs to this step. If not included, the step is assumed to be an initial step.

    Attributes
    ----------
    name : str
        Name of the step within the scope of the pipeline.
    registered_model_version : :class:`~verta.registry.entities.RegisteredModelVersion`
        Registered model version run by this step.
    predecessors : set
        Set of PipelineSteps whose outputs will be treated as inputs to this step.
    """

    def __init__(
        self,
        name: str,
        registered_model_version: RegisteredModelVersion,
        predecessors: Optional[
            Union[List["PipelineStep"], Set["PipelineStep"], Tuple["PipelineStep"]]
        ] = None,  # Optional because it could be the first step with no predecessors
    ):
        self._name = self.set_name(name)
        self._registered_model_version = self.set_registered_model_version(
            registered_model_version
        )
        self._predecessors = self.set_predecessors(predecessors)
        self._registered_model: RegisteredModel = self._get_registered_model()

    def __repr__(self) -> str:
        return "\n    ".join(
            (
                "PipelineStep:",
                f"step name: {self.name}",
                f"registered_model: {self._registered_model.name}",
                f"registered_model_id: {self._registered_model.id}",
                f"registered_model_version: {self.registered_model_version.name}",
                f"registered_model_version_id: {self.registered_model_version.id}",
                f"predecessors: {[s.name for s in self.predecessors]}",
            )
        )

    @property
    def registered_model_version(self) -> RegisteredModelVersion:
        return self._registered_model_version

    @registered_model_version.setter
    def registered_model_version(self, value) -> None:
        """Raise a more informative error than the default."""
        raise AttributeError(
            "can't set attribute 'registered_model_version'; please use set_registered_model_version()"
        )

    def set_registered_model_version(
        self, registered_model_version: RegisteredModelVersion
    ) -> RegisteredModelVersion:
        """Set the registered model version associated with this step.

        Parameters
        ----------
        registered_model_version : :class:`~verta.registry.entities.RegisteredModelVersion`
            Registered model version to use for the step.

        Returns
        -------
        RegisteredModelVersion
            The new registered model version now set for this step.

        Raises
        ------
        TypeError
            If the provided value is not type RegisteredModelVersion.

        """
        if not isinstance(registered_model_version, RegisteredModelVersion):
            raise TypeError(
                f"registered_model_version must be a RegisteredModelVersion object, "
                f"not {type(registered_model_version)}"
            )
        self._registered_model_version = registered_model_version
        self._registered_model = self._get_registered_model()
        return self.registered_model_version

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

        Returns
        -------
        str
            The new name now set for this step.

        Raises
        ------
        TypeError
            If the provided value for ``name`` is not type str.

        """
        if not isinstance(name, str):
            raise TypeError(f"name must be a string, not {type(name)}")
        self._name = name
        return self.name

    @property
    def predecessors(self) -> Set["PipelineStep"]:
        return self._predecessors

    @predecessors.setter
    def predecessors(self, value) -> None:
        """Raise a more informative error than the default."""
        raise AttributeError(
            "can't set attribute 'predecessors'; please use set_predecessors()"
        )

    def set_predecessors(
        self,
        steps: Optional[
            Union[List["PipelineStep"], Set["PipelineStep"], Tuple["PipelineStep"]]
        ] = None,
    ) -> Set["PipelineStep"]:
        """Set the predecessors associated with this step.

        Parameters
        ----------
        steps : list, set, or tuple, optional
            List, set, or tuple of PipelineStep objects whose outputs will be treated as
            inputs to this step. All options are converted to a set, so order is irrelevant
            and duplicates are removed. An empty set is used if no input is provided.

        Returns
        -------
        set of PipelineStep
            The new set of predecessors now set for this step.

        Raises
        ------
        TypeError
            If the provided value for ``steps`` is not a set of PipelineStep objects.
        """
        if steps:
            self._predecessors = set(self._validate_predecessors(steps))
            return self.predecessors
        self._predecessors = set()
        return self.predecessors

    @staticmethod
    def _validate_predecessors(
        predecessors: Union[
            Set["PipelineStep"], List["PipelineStep"], Tuple["PipelineStep"]
        ]
    ) -> Set["PipelineStep"]:
        """Validate that the provided predecessors are a set of PipelineStep objects.

        Parameters
        ----------
        predecessors : set
            Set of PipelineStep objects whose outputs will be treated as inputs to
            this step.
        """
        if not isinstance(predecessors, (set, list, tuple)):
            raise TypeError(
                f"steps must be type list, set, or tuple, not {type(predecessors)}"
            )
        for step in predecessors:
            if not isinstance(step, PipelineStep):
                raise TypeError(
                    f"individual predecessors of a PipelineStep must be type"
                    f" PipelineStep, not {type(step)}."
                )
        return predecessors

    def _get_registered_model(self) -> RegisteredModel:
        """Fetch the registered model associated with this step's model version.

        This is to provide important context to the user when a registered
        pipeline is fetched from the backend.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModel`
        """
        rm = RegisteredModel._get_by_id(
            id=self._registered_model_version.registered_model_id,
            conn=self.registered_model_version._conn,
            conf=self.registered_model_version._conf,
        )
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
            Specification dictionary for the whole pipeline.
        conn : :class:`~verta._internal_utils._utils.Connection`
            Connection object for fetching the model version associated with each step.
        conf: :class:`~verta._internal_utils._utils.Configuration`
            Configuration object for fetching the model version associated with each
            step.

        Returns
        -------
        set of :class:`~verta._pipelines.PipelineStep`
            Set of steps in the pipeline spec as PipelineStep objects.
        """
        steps: Set["PipelineStep"] = set()
        for step in pipeline_definition["steps"]:
            steps.add(
                cls(
                    name=step["name"],
                    registered_model_version=RegisteredModelVersion._get_by_id(
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

    def _to_graph_spec(self) -> Dict[str, Any]:
        """Return a dictionary representation of predecessors for this step,
        formatted for a pipeline definition.

        This is fed to the backend as 'graph' in our PipelineDefinition schema
        """
        return {
            "name": self.name,
            "predecessors": [s.name for s in self.predecessors],
        }

    def _to_step_spec(self) -> Dict[str, Any]:
        """Return a dictionary representation of this step, formatted for a
         pipeline definition.

        This is fed to the backend as 'steps' in our PipelineDefinition schema
        """
        return {
            "name": self.name,
            "model_version_id": self.registered_model_version.id,
        }
