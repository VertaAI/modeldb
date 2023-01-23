# -*- coding: utf-8 -*-
"""Utilities for data versioning."""

from verta._internal_utils import documentation

from ._dataset import (
    _Dataset,
    Component,
)
from ._path import Path
from ._hdfs import HDFSPath
from ._s3 import S3


documentation.reassign_module(
    [
        Component,
        Path,
        HDFSPath,
        S3,
    ],
    module_name=__name__,
)
