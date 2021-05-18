# -*- coding: utf-8 -*-

from verta._internal_utils import documentation

from .dataset import Dataset
from .datasets import Datasets
from .dataset_version import DatasetVersion
from .dataset_versions import DatasetVersions


documentation.reassign_module(
    [
        Dataset,
        Datasets,
        DatasetVersion,
        DatasetVersions,
    ],
    module_name=__name__,
)
