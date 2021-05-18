# -*- coding: utf-8 -*-

from verta._internal_utils import documentation

from ._dataset import _Dataset
from ._path import Path
from ._hdfs import HDFSPath
from ._s3 import S3


documentation.reassign_module(
    [
        Path,
        HDFSPath,
        S3,
    ],
    module_name=__name__,
)
