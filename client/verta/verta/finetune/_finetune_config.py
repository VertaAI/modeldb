# -*- coding: utf-8 -*-

import abc
import dataclasses
from typing import Any, Dict


class _FinetuneConfig(abc.ABC):
    """Abstract base class for fine-tuning algorithm configurations."""

    @abc.abstractmethod
    def __post_init__(self):
        """Validate types and values of fields on `self`."""
        raise NotImplementedError

    @property
    @abc.abstractmethod
    def _JOB_DICT_KEY(self) -> str:
        """Return the key in FineTuningJobCreate/FineTuningJobResponse for this config.

        e.g. ``"lora_config_parameters"`` for ``LoraConfig``.

        """
        raise NotImplementedError

    def _as_dict(self) -> Dict[str, Any]:
        """Return the item in FineTuningJobCreate for this config.

        Examples
        --------
        .. code-block:: python
            :emphasize-lines: 1

            data[self._JOB_DICT_KEY] = config._as_dict()
            url = "{}://{}/api/v1/deployment/workspace/{}/finetuning-job".format(
                conn.scheme, conn.socket, workspace
            )
            response = _utils.make_request("POST", url, conn, json=data)

        """
        return dataclasses.asdict(self)

    @classmethod
    def _from_job_dict(cls, job_dict: Dict[str, Any]) -> "_FinetuneConfig":
        """Return a fine-tune config object from FineTuningJobResponse.

        Examples
        --------
        .. code-block:: python
            :emphasize-lines: 6

            url = "{}://{}/api/v1/deployment/workspace/{}/finetuning-job/{}".format(
                conn.scheme, conn.socket, workspace, job_id
            )
            response = _utils.make_request("GET", url, conn)
            _utils.raise_for_http_error(response)
            config = _FinetuningConfig._from_job_dict(response.json())

        """
        for subcls in cls.__subclasses__():
            config_dict = job_dict.get(subcls._request_body_key)
            if config_dict:
                hydrated_config_dict = {
                    field.name: config_dict.get(
                        field.name,
                        field.type(),  # zero-value if not present
                    )
                    for field in dataclasses.fields(subcls)
                }
                if any(hydrated_config_dict.values()):
                    # config_dict has at least one non-zero-value item
                    config = subcls(**hydrated_config_dict)
                    break
        else:
            raise ValueError("fine-tune config not found")

        return config
