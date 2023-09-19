# -*- coding: utf-8 -*-
"""Utilities for model fine-tuning."""

from verta._internal_utils import documentation
from verta.tracking.entities._deployable_entity import _RESERVED_ATTR_PREFIX

from ._finetuning_config import _FinetuningConfig
from ._lora_config import LoraConfig


_TRACKING_NAME_SUFFIX = " Fine-Tuning"  # append to RM and RMV names for project and run
_EXPERIMENT_NAME = "Fine-Tuning"
_TRAIN_DATASET_NAME = "train"
_EVAL_DATASET_NAME = "eval"
_TEST_DATASET_NAME = "test"
_FINETUNE_BASE_RMV_ATTR_KEY = f"{_RESERVED_ATTR_PREFIX}FINETUNE_BASE"
_FINETUNE_ATTR_KEY = f"{_RESERVED_ATTR_PREFIX}FINETUNE"


documentation.reassign_module(
    [
        _FinetuningConfig,
        LoraConfig,
    ],
    module_name=__name__,
)
