# -*- coding: utf-8 -*-

from __future__ import print_function

from ..external import six
from ..external.six.moves.urllib.parse import urlparse  # pylint: disable=import-error, no-name-in-module

from .._protos.public.modeldb.versioning import Dataset_pb2 as _DatasetService

from .._internal_utils import _utils

from . import _dataset


class S3(_dataset._Dataset):
    """
    Captures metadata about S3 objects.

    Parameters
    ----------
    paths : list of str
        List of S3 object URLs of the form "s3://<bucket-name>/<key>" or bucket URLs of the form
        "s3://<bucket-name>".

    Examples
    --------
    .. code-block:: python

        from verta.dataset import S3
        dataset1 = S3([
            "s3://verta-starter/census-train.csv",
            "s3://verta-starter/census-test.csv",
        ])
        dataset2 = S3([
            "s3://verta-starter",
        ])

    """
    _S3_PATH = "s3://{}/{}"

    def __init__(self, paths):
        if isinstance(paths, six.string_types):
            paths = [paths]

        super(S3, self).__init__()

        obj_paths_to_metadata = dict()  # prevent duplicate objects
        for path in paths:
            bucket_name, key = self._parse_s3_url(path)
            obj_paths_to_metadata.update({
                obj_metadata.path.path: obj_metadata
                for obj_metadata
                in self._get_s3_metadata(bucket_name, key)
            })

        s3_metadata = six.viewvalues(obj_paths_to_metadata)
        self._msg.s3.components.extend(s3_metadata)  # pylint: disable=no-member

    @staticmethod
    def _parse_s3_url(path):
        url_components = urlparse(path, allow_fragments=False)
        if url_components.scheme != 's3':
            raise ValueError("`path` \"{}\" must be either \"s3://<bucket-name>\""
                             " or \"s3://<bucket-name>/<key>\"".format(path))

        bucket_name = url_components.netloc
        key = url_components.path
        if key.startswith('/'):
            key = key[1:]
        if key == "":
            key = None

        return bucket_name, key

    @classmethod
    def _get_s3_metadata(cls, bucket_name, key=None):
        try:
            import boto3
        except ImportError:
            e = ImportError("Boto 3 is not installed; try `pip install boto3`")
            six.raise_from(e, None)
        s3 = boto3.client('s3')

        # TODO: handle prefixes
        if key is None:
            # TODO: handle `bucket_name` not found
            for obj in s3.list_objects(Bucket=bucket_name)['Contents']:
                yield cls._get_s3_obj_metadata(obj, bucket_name, obj['Key'])
        else:
            # TODO: handle `key` not found
            obj = s3.head_object(Bucket=bucket_name, Key=key)
            yield cls._get_s3_obj_metadata(obj, bucket_name, key)

    @classmethod
    def _get_s3_obj_metadata(cls, obj, bucket_name, key):
        # pylint: disable=no-member
        msg = _DatasetService.S3DatasetComponentBlob()
        msg.path.path = cls._S3_PATH.format(bucket_name, key)
        msg.path.size = obj.get('Size') or obj['ContentLength']
        msg.path.last_modified_at_source = _utils.timestamp_to_ms(_utils.ensure_timestamp(obj['LastModified']))
        msg.path.md5 = obj['ETag'].strip('"')

        return msg
