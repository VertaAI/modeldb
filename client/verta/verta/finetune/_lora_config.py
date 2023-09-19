# -*- coding: utf-8 -*-

import dataclasses

from ._finetuning_config import _FinetuningConfig


@dataclasses.dataclass(order=False, frozen=True)
class LoraConfig(_FinetuningConfig):
    """LoRA fine-tuning configuration.

    For use with :meth:`RegisteredModelVersion.finetune() <verta.registry.entities.RegisteredModelVersion.finetune>`

    Parameters
    ----------
    alpha : positive int, default 32
        Scaling factor for update matrices.
    dropout : float between 0.0 and 1.0 inclusive, default 0.0
        Dropout probability for LoRA layers.
    r : positive int, default 8
        Rank of update matrices.

    """

    alpha: int = 32
    dropout: float = 0.0
    r: int = 8

    def __post_init__(self) -> None:
        msg = f"`alpha` must be a positive integer, not {self.alpha}"
        if not isinstance(self.alpha, int):
            raise TypeError(msg)
        if not self.alpha > 0:
            raise ValueError(msg)

        msg = f"`dropout` must be a float between 0.0 and 1.0 inclusive, not {self.dropout}"
        if not isinstance(self.dropout, float):
            raise TypeError(msg)
        if not 0 <= self.dropout <= 1:
            raise ValueError(msg)

        msg = f"`r` must be a positive integer, not {self.r}"
        if not isinstance(self.r, int):
            raise TypeError(msg)
        if not self.r > 0:
            raise ValueError(msg)

    @property
    def _JOB_DICT_KEY(self) -> str:
        return "lora_config_parameters"
