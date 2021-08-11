# -*- coding: utf-8 -*-
"""Standalone entities for data versioning."""

from verta._internal_utils import documentation

from ._dataset import Dataset
from ._datasets import Datasets
from ._dataset_version import DatasetVersion
from ._dataset_versions import DatasetVersions


documentation.reassign_module(
    [
        Dataset,
        Datasets,
        DatasetVersion,
        DatasetVersions,
    ],
    module_name=__name__,
)
