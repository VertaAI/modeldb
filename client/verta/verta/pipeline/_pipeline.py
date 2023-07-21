from typing import Any, Dict, List, Optional

from verta.pipeline._pipelinestep import PipelineStep
from .._internal_utils._utils import Connection, make_request, raise_for_http_error


class Pipeline:
    """
    A collection of PipelineSteps to be run as part of a single inference pipeline.
    There should not be a need to instantiate this class directly; please use
    :meth:`Client.create_pipeline() <verta.Client.create_piepline>`.

    Parameters
    ----------
    name : str
        Name of the pipeline.
    steps : list of :class:`~verta.deployment.PipelineStep`, optional
        All possible steps of the Pipline.

    Attributes
    ----------
        id
            Auto-assigned ID of this Pipeline, if deployed to Verta.
        name: str
            Name of this pipeline.
        steps: list of :class:`~verta.deployment.PipelineStep`
            List of PipelineSteps comprising all possible steps in the Pipline.
        status: dict
            Current status of this Pipeline, including status of all component steps.
    """

    def __init__(self, name: str, steps: Optional[List[PipelineStep]] = None):
        self._name = name
        self._steps = steps
        self._msg: Optional[Dict[str, Any]] = None

    def __repr__(self):
        return "\n".join(
            (
                f"\npipeline name: {self.name}",
                f"pipeline id: {self.id}",
                f"status: {self.status}",
                f"steps: \n\n{self._format_steps()}",
            )
        )

    def _format_steps(self):
        """Format steps for __repr__."""
        if not self.steps:
            return "None"
        return "\n\n".join([str(s) for s in self.steps])

    @property
    def id(self):
        if self._msg is None:
            return "No ID. (Pipeline not deployed)"
        return self._msg["id"]

    @property
    def name(self):
        return self._name

    @property
    def steps(self):
        if not self._steps:
            raise RuntimeError(f"Pipeline {self.name} does not have any steps.")
        return self._steps

    @property
    def status(self):
        if self._msg is None:
            return "No status available. (Pipeline not deployed)"
        return self._status_from_msg()

    def add_steps(self, steps: List[PipelineStep]) -> None:
        """
        Add steps to this Pipeline.

        Parameters
        ----------
        steps : list of :class:`~verta.deployment.PipelineStep`
            Steps to add to this Pipeline.
        """
        for step in steps:
            if not isinstance(step, PipelineStep):
                raise TypeError(
                    f"steps must be an instance of the verta.pipelines.PipelineStep class, "
                    f"not {type(step)}"
                )
        if not self.steps:
            self.steps = steps
        else:
            self.steps.extend(steps)
            print("Steps successfully added to Pipeline.")

    def remove_steps(self, steps: List[str]) -> None:
        """
        Remove steps from this Pipeline, using their ``name`` attributes.

        Parameters
        ----------
        steps : list of str
            Names of the steps to remove from this Pipeline.
        """
        for step in steps:
            if step not in [s.name for s in self.steps]:
                raise ValueError(f"Step {step} not found in Pipeline {self.name}")
            step_obj = [s for s in self.steps if s.name == step][0]
            self.steps.remove(step_obj)

    def _status_from_msg(self):
        pipeline_status = self._msg["status"]
        steps_status = {}
        for step in self.steps:
            steps_status.update({step["name"]: step["status"]})
        return {"pipeline_status": pipeline_status, "steps_status": steps_status}

    @classmethod
    def _from_spec(cls, pipeline_spec: Dict[str, Any]):
        """
        Create a Pipeline instance from a specification dict.

        Parameters
        ----------
        pipeline_spec : dict
            Specification dict from which to create the Pipeline.
        """
        return cls(
            name=pipeline_spec["pipeline_name"],
            steps=[
                PipelineStep.from_pipeline_spec(step_spec)
                for step_spec in pipeline_spec["steps"]
            ],
        )

    def _to_spec(self) -> Dict[str, Any]:
        """Convert this Pipeline to a specification dict."""
        return {
            "graph": [step._to_graph_spec() for step in self.steps],
            "pipeline_name": self.name,
            "steps": [step._to_steps_spec() for step in self.steps],
        }

    def _register_pipeline(self, conn: Connection, update: bool) -> None:
        """
        Register this Pipeline with the Verta backend.

        Parameters
        ----------
        conn : :class:`~verta.connection.Connection`
            Connection object to the backend.
        update : bool
            Whether to update the Pipeline if it already registered.
        """
        request_url = (
            f"{conn.scheme}://{conn.socket}/api/v1/modeldb/development/pipelines"
        )
        if update:
            request_url += f"/{self.id}"
        response = make_request("PUT", request_url, conn, json=self._to_spec())
        if response:
            self._msg = response.json()
            print("pipeline registered successfully.")
        raise_for_http_error(response)

    def _get_status(self, conn: Connection) -> str:
        """
        Attempt to retrieve a Pipeline from the Verta backend, by ID.

        Parameters
        ----------
        conn : :class:`~verta.connection.Connection`
            Connection object to the backend.
        """
        request_url = (
            f"{conn.scheme}://{conn.socket}/api/v1/development/pipelines/{self.id}"
        )
        response = make_request("GET", request_url, conn)
        if response.status_code == 200:
            self._msg = response.json()
        raise_for_http_error(response)
        return self.status
