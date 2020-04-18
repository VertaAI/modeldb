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

    If your S3 object requires additional information to identify it, such as its version ID, you
    can use :meth:`S3.location`.

    Parameters
    ----------
    paths : list
        List of S3 URLs of the form ``"s3://<bucket-name>"`` or ``"s3://<bucket-name>/<key>"``, or
        objects returned by :meth:`S3.location`.

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
        dataset3 = S3([
            S3.location("s3://verta-starter/census-train.csv",
                        version_id="xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"),
        ])

    """
    _S3_PATH = "s3://{}/{}"

    def __init__(self, paths):
        if isinstance(paths, (six.string_types, S3Location)):
            paths = [paths]

        super(S3, self).__init__()

        obj_paths_to_metadata = dict()  # prevent duplicate objects
        for path in paths:
            # convert paths to S3Location
            if isinstance(path, six.string_types):
                s3_loc = S3Location(path)
            elif isinstance(path, S3Location):
                s3_loc = path
            else:
                raise TypeError(
                    "`paths` must contain either str or S3Location,"
                    " not {} ({})".format(type(path), path)
                )

            obj_paths_to_metadata.update({
                obj_metadata.path.path: obj_metadata
                for obj_metadata
                in self._get_s3_loc_metadata(s3_loc)
            })

        s3_metadata = six.viewvalues(obj_paths_to_metadata)
        self._msg.s3.components.extend(s3_metadata)  # pylint: disable=no-member

    def __repr__(self):
        lines = ["S3 Version"]
        components = sorted(
            self._msg.s3.components,
            key=lambda component_msg: component_msg.path.path,
        )
        for component in components:
            lines.extend(self._path_component_to_repr_lines(component.path))

        return "\n    ".join(lines)

    @classmethod
    def _get_s3_loc_metadata(cls, s3_loc):
        try:
            import boto3
        except ImportError:
            e = ImportError("Boto 3 is not installed; try `pip install boto3`")
            six.raise_from(e, None)
        s3 = boto3.client('s3')

        # TODO: handle prefixes
        if s3_loc.key is None:
            # TODO: handle `bucket_name` not found
            for obj in s3.list_object_versions(Bucket=s3_loc.bucket)['Versions']:
                if obj['IsLatest']:
                    yield cls._get_s3_obj_metadata(obj, s3_loc.bucket, obj['Key'])
        else:
            # TODO: handle `key` not found
            if s3_loc.version_id is not None:
                # TODO: handle `version_id` not found
                obj = s3.head_object(Bucket=s3_loc.bucket, Key=s3_loc.key, VersionId=s3_loc.version_id)
            else:
                obj = s3.head_object(Bucket=s3_loc.bucket, Key=s3_loc.key)
            yield cls._get_s3_obj_metadata(obj, s3_loc.bucket, s3_loc.key)

    @classmethod
    def _get_s3_obj_metadata(cls, obj, bucket_name, key):
        # pylint: disable=no-member
        msg = _DatasetService.S3DatasetComponentBlob()
        msg.path.path = cls._S3_PATH.format(bucket_name, key)
        msg.path.size = obj.get('Size') or obj.get('ContentLength') or 0
        msg.path.last_modified_at_source = _utils.timestamp_to_ms(_utils.ensure_timestamp(obj['LastModified']))
        msg.path.md5 = obj['ETag'].strip('"')
        if obj.get('VersionId', 'null') != 'null':  # S3's API returns 'null' when there's no version ID
            msg.s3_version_id = obj['VersionId']

        return msg

    @staticmethod
    def location(path, version_id=None):
        """
        Returns an object describing an S3 location that can be passed into a new :class:`S3`.

        Parameters
        ----------
        path : str
            S3 URL of the form ``"s3://<bucket-name>"`` or ``"s3://<bucket-name>/<key>"``.
        version_id : str, optional
            ID of an S3 object version.

        Returns
        -------
        :class:`S3Location`
            A location in S3.

        Raises
        ------
        ValueError
            If `version_id` is provided but `path` represents a bucket rather than a single object.

        """
        return S3Location(path, version_id)


class S3Location(object):
    def __init__(self, path, version_id=None):
        bucket, key = self._parse_s3_url(path)
        if (version_id is not None) and (key is None):
            raise ValueError(
                "`version_id` can only be provided if"
                " `path` specifies a single S3 object"
            )

        self.bucket = bucket
        self.key = key
        self.version_id = version_id

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
