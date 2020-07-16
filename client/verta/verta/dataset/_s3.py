# -*- coding: utf-8 -*-

from __future__ import print_function

import os
import pathlib2
import tempfile

from ..external import six
from ..external.six.moves.urllib.parse import urlparse  # pylint: disable=import-error, no-name-in-module

from .._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService

from .._internal_utils import _artifact_utils
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
    enable_mdb_versioning : bool, default False
        Whether to upload the data itself to ModelDB to enable managed data versioning.

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

    .. describe:: dataset += other

        Updates the dataset, adding paths from ``other``.

    .. describe:: dataset + other + ...

        Returns a new dataset with paths from the dataset and all others.

    """
    _S3_PATH = "s3://{}/{}"

    def __init__(self, paths, enable_mdb_versioning=False):
        if isinstance(paths, (six.string_types, S3Location)):
            paths = [paths]

        super(S3, self).__init__(enable_mdb_versioning=enable_mdb_versioning)

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

            self._components_map.update({
                component.path: component
                for component
                in self._get_s3_components(s3_loc)
            })

    @classmethod
    def _from_proto(cls, blob_msg):
        obj = cls(paths=[])

        for component_msg in blob_msg.dataset.s3.components:
            component = _dataset.S3Component._from_proto(component_msg)
            obj._components_map[component.path] = component

        return obj

    def _as_proto(self):
        blob_msg = _VersioningService.Blob()

        for component in self._components_map.values():
            component_msg = component._as_proto()
            blob_msg.dataset.s3.components.append(component_msg)

        return blob_msg

    @classmethod
    def _get_s3_components(cls, s3_loc):
        try:
            import boto3
        except ImportError:
            e = ImportError("Boto 3 is not installed; try `pip install boto3`")
            six.raise_from(e, None)
        s3 = boto3.client('s3')

        if (s3_loc.key is None  # bucket
                or s3_loc.key.endswith('/')):  # folder
            if s3_loc.key is None:
                # TODO: handle `bucket_name` not found
                obj_versions = s3.list_object_versions(Bucket=s3_loc.bucket)
            else:
                obj_versions = s3.list_object_versions(Bucket=s3_loc.bucket, Prefix=s3_loc.key)
                if 'Versions' not in obj_versions:  # boto3 doesn't error, so we have to catch this
                    s3_path = cls._S3_PATH.format(s3_loc.bucket, s3_loc.key)
                    raise ValueError("folder {} not found".format(s3_path))

            for obj in obj_versions['Versions']:
                if obj['Key'].endswith('/'):  # folder, not object
                    continue
                if not obj['IsLatest']:
                    continue
                yield cls._s3_obj_to_component(obj, s3_loc.bucket, obj['Key'])
        else:
            # TODO: handle `key` not found
            if s3_loc.version_id is not None:
                # TODO: handle `version_id` not found
                obj = s3.head_object(Bucket=s3_loc.bucket, Key=s3_loc.key, VersionId=s3_loc.version_id)
            else:
                obj = s3.head_object(Bucket=s3_loc.bucket, Key=s3_loc.key)
            yield cls._s3_obj_to_component(obj, s3_loc.bucket, s3_loc.key)

    @classmethod
    def _s3_obj_to_component(cls, obj, bucket_name, key):
        component = _dataset.S3Component(
            path=cls._S3_PATH.format(bucket_name, key),
            size=obj.get('Size') or obj.get('ContentLength') or 0,
            last_modified=_utils.timestamp_to_ms(_utils.ensure_timestamp(obj['LastModified'])),
            md5=obj['ETag'].strip('"'),
        )
        if obj.get('VersionId', 'null') != 'null':  # S3's API returns 'null' when there's no version ID
            component.s3_version_id = obj['VersionId']

        return component

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

    def _prepare_components_to_upload(self):
        """
        Downloads files from S3 and tracks them for upload to ModelDB.

        This method does nothing if ModelDB-managed versioning was not enabled.

        """
        if not self._mdb_versioned:
            return

        try:
            import boto3
        except ImportError:
            e = ImportError("Boto 3 is not installed; try `pip install boto3`")
            six.raise_from(e, None)
        s3 = boto3.client('s3')

        # download files to local disk
        for component in self._components_map.values():
            s3_loc = S3Location(component.path, component.s3_version_id)

            # download to file in ~/.verta/temp/
            tempdir = os.path.join(_utils.HOME_VERTA_DIR, "temp")
            pathlib2.Path(tempdir).mkdir(parents=True, exist_ok=True)
            print("downloading {} from S3".format(component.path))
            with tempfile.NamedTemporaryFile('w+b', dir=tempdir, delete=False) as tempf:
                s3.download_fileobj(
                    Bucket=s3_loc.bucket,
                    Key=s3_loc.key,
                    ExtraArgs={'VersionId': s3_loc.version_id} if s3_loc.version_id else None,
                    Fileobj=tempf,
                )
            print("download complete")

            # track which downloaded file this component corresponds to
            component._local_path = tempf.name

            # add MDB path to component blob
            with open(tempf.name, 'rb') as f:
                artifact_hash = _artifact_utils.calc_sha256(f)
            component._internal_versioned_path = artifact_hash + '/' + s3_loc.key

    def _clean_up_uploaded_components(self):
        """
        Deletes temporary files that had been downloaded for ModelDB-managed versioning.

        This method does nothing if ModelDB-managed versioning was not enabled.

        """
        if not self._mdb_versioned:
            return

        for component in self._components_map.values():
            if component._local_path and os.path.isfile(component._local_path):
                os.remove(component._local_path)

    def add(self, paths):
        """
        Adds `paths` to this dataset.

        Parameters
        ----------
        paths : list
            List of S3 URLs of the form ``"s3://<bucket-name>"`` or ``"s3://<bucket-name>/<key>"``, or
            objects returned by :meth:`S3.location`.

        """
        # re-use logic in __init__
        other = self.__class__(
            paths=paths,
            enable_mdb_versioning=self._mdb_versioned,
        )

        self += other


class S3Location(object):
    # TODO: handle prefixes
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
