# -*- coding: utf-8 -*-

from __future__ import print_function

import warnings

from verta.external import six

from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta._protos.public.modeldb import DatasetVersionService_pb2 as _DatasetVersionService
from verta._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService

from verta.tracking.entities import _entity
from verta.repository import _commit
from verta._internal_utils import (
    _artifact_utils,
    _utils,
)


class DatasetVersion(_entity._ModelDBEntity):
    """
    Object representing a ModelDB dataset version.

    .. versionchanged:: 0.16.0
        The dataset versioning interface was updated for flexibility,
        robustness, and consistency with other ModelDB entities.

    This class provides read/write functionality for dataset version metadata and access to its content.

    There should not be a need to instantiate this class directly; please use
    :meth:`Dataset.create_version() <verta.dataset.entities.Dataset.create_version>`.

    Attributes
    ----------
    id : str
        ID of this dataset version.
    version : int
        Version number of this dataset version.
    dataset_id : str
        ID of this version's dataset.
    parent_id : str
        ID of this version's preceding version.

    """
    def __init__(self, conn, conf, msg):
        super(DatasetVersion, self).__init__(conn, conf, _DatasetVersionService, "dataset-version", msg)

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg

        return '\n'.join((
            "version: {}".format(msg.version),
            # TODO: "url: {}://{}/{}/datasets/{}/summary".format(self._conn.scheme, self._conn.socket, self.workspace, self.id),
            "time created: {}".format(_utils.timestamp_to_str(int(msg.time_logged))),
            "time updated: {}".format(_utils.timestamp_to_str(int(msg.time_updated))),
            "description: {}".format(msg.description),
            "tags: {}".format(msg.tags),
            "attributes: {}".format(_utils.unravel_key_values(msg.attributes)),
            "id: {}".format(msg.id),
            "content:",
            "{}".format(self.get_content()),  # TODO: increase indentation
        ))

    @property
    def version(self):
        self._refresh_cache()
        return self._msg.version

    @property
    def dataset_id(self):
        self._refresh_cache()
        return self._msg.dataset_id

    @property
    def parent_id(self):
        self._refresh_cache()
        return self._msg.parent_id

    @classmethod
    def _generate_default_name(cls):
        return "DatasetVersion {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _DatasetVersionService.GetDatasetVersionById
        msg = Message(id=id)
        endpoint = "/api/v1/modeldb/dataset-version/getDatasetVersionById"
        response = conn.make_proto_request("GET", endpoint, params=msg)
        return conn.maybe_proto_response(response, Message.Response).dataset_version

    @classmethod
    def _get_latest_version_by_dataset_id(cls, conn, conf, dataset_id):
        Message = _DatasetVersionService.GetLatestDatasetVersionByDatasetId
        msg = Message(dataset_id=dataset_id)
        endpoint = "/api/v1/modeldb/dataset-version/getLatestDatasetVersionByDatasetId"
        response = conn.make_proto_request("GET", endpoint, params=msg)

        dataset_version = conn.must_proto_response(response, Message.Response).dataset_version
        print("got existing dataset version: {}".format(dataset_version.id))
        return cls(conn, conf, dataset_version)

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        raise NotImplementedError("cannot get DatasetVersion by name")

    @classmethod
    def _create(cls, conn, conf, *args, **kwargs):
        """
        This override exists solely to enable managed versioning:
        The artifact upload methods require an object (`self`).

        """
        dataset_blob = kwargs['dataset_blob']  # definitely exists: required in Dataset.create_version()

        # prepare ModelDB-versionend components for upload (after Dataset Version creation)
        if dataset_blob._mdb_versioned:
            dataset_blob._prepare_components_to_upload()

        obj = super(DatasetVersion, cls)._create(conn, conf, *args, **kwargs)

        # upload ModelDB-versioned components
        if dataset_blob._mdb_versioned:
            for component in dataset_blob._components_map.values():
                if component._internal_versioned_path:
                    with open(component._local_path, 'rb') as f:
                        obj._upload_artifact(component.path, f)  # pylint: disable=no-member

        return obj

    @classmethod
    def _create_proto_internal(cls, conn, dataset, dataset_blob, desc=None, tags=None, attrs=None, time_logged=None, time_updated=None):
        Message = _DatasetVersionService.CreateDatasetVersion
        msg = Message(dataset_id=dataset.id, description=desc, tags=tags, attributes=attrs, time_created=time_updated,
                      dataset_blob=dataset_blob._as_proto().dataset)

        endpoint = "/api/v1/modeldb/dataset-version/createDatasetVersion"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        dataset_version = conn.must_proto_response(response, Message.Response).dataset_version

        print("created new Dataset Version: {} for {}".format(dataset_version.version, dataset.name))
        return dataset_version

    def get_content(self):
        """
        Returns the content of this dataset version.

        Returns
        -------
        dataset : :mod:`~verta.dataset`
            Dataset content.

        """
        self._refresh_cache()

        # create wrapper blob msg so we can reuse the repository system's proto-to-obj
        blob = _VersioningService.Blob()
        blob.dataset.CopyFrom(self._msg.dataset_blob)
        content = _commit.blob_msg_to_object(blob)

        # for _Dataset.download()
        content._set_dataset_version(self)

        return content

    def list_components(self):  # from legacy DatasetVersion
        """
        Shorthand for ``get_content().list_components()``.

        """
        return self.get_content().list_components()

    def set_description(self, desc):
        """
        Sets the description of this dataset version.

        Parameters
        ----------
        desc : str
            Description to set.

        """
        Message = _DatasetVersionService.UpdateDatasetVersionDescription
        msg = Message(id=self.id, description=desc)
        endpoint = "/api/v1/modeldb/dataset-version/updateDatasetVersionDescription"
        self._update(msg, Message.Response, endpoint, "POST")

    def get_description(self):
        """
        Gets the description of this dataset version.

        Returns
        -------
        str
            Description of this dataset version.

        """
        self._refresh_cache()
        return self._msg.description

    def add_tag(self, tag):
        """
        Adds a tag to this dataset version.

        Parameters
        ----------
        tag : str
            Tag to add.

        """
        if not isinstance(tag, six.string_types):
            raise TypeError("`tag` must be a string")

        self.add_tags([tag])

    def add_tags(self, tags):
        """
        Adds multiple tags to this dataset version.

        Parameters
        ----------
        tags : list of str
            Tags to add.

        """
        tags = _utils.as_list_of_str(tags)
        Message = _DatasetVersionService.AddDatasetVersionTags
        msg = Message(id=self.id, tags=tags)
        endpoint = "/api/v1/modeldb/dataset-version/addDatasetVersionTags"
        self._update(msg, Message.Response, endpoint, "POST")

    def get_tags(self):
        """
        Gets all tags from this dataset version.

        Returns
        -------
        list of str
            All tags.

        """
        self._refresh_cache()
        return self._msg.tags

    def del_tag(self, tag):
        """
        Deletes a tag from this dataset version.

        This method will not raise an error if the tag does not exist.

        Parameters
        ----------
        tag : str
            Tag to delete.

        """
        if not isinstance(tag, six.string_types):
            raise TypeError("`tag` must be a string")

        Message = _DatasetVersionService.DeleteDatasetVersionTags
        msg = Message(id=self.id, tags=[tag])
        endpoint = "/api/v1/modeldb/dataset-version/deleteDatasetVersionTags"
        self._update(msg, Message.Response, endpoint, "DELETE")

    def add_attribute(self, key, value):
        """
        Adds an attribute to this dataset version.

        Parameters
        ----------
        key : str
            Name of the attribute.
        value : one of {None, bool, float, int, str, list, dict}
            Value of the attribute.

        """
        self.add_attributes({key: value})

    def add_attributes(self, attrs):
        """
        Adds potentially multiple attributes to this dataset version.

        Parameters
        ----------
        attributes : dict of str to {None, bool, float, int, str, list, dict}
            Attributes.

        """
        # validate all keys first
        for key in six.viewkeys(attrs):
            _utils.validate_flat_key(key)

        # build KeyValues
        attribute_keyvals = []
        for key, value in six.viewitems(attrs):
            attribute_keyvals.append(_CommonCommonService.KeyValue(key=key,
                                                                   value=_utils.python_to_val_proto(
                                                                       value,
                                                                       allow_collection=True)))

        Message = _DatasetVersionService.AddDatasetVersionAttributes
        msg = Message(id=self.id, attributes=attribute_keyvals)
        endpoint = "/api/v1/modeldb/dataset-version/addDatasetVersionAttributes"
        self._update(msg, Message.Response, endpoint, "POST")

    def get_attribute(self, key):
        """
        Gets the attribute with name `key` from this dataset version.

        Parameters
        ----------
        key : str
            Name of the attribute.

        Returns
        -------
        one of {None, bool, float, int, str}
            Value of the attribute.

        """
        _utils.validate_flat_key(key)
        attributes = self.get_attributes()

        try:
            return attributes[key]
        except KeyError:
            six.raise_from(KeyError("no attribute found with key {}".format(key)), None)

    def get_attributes(self):
        """
        Gets all attributes from this dataset version.

        Returns
        -------
        dict of str to {None, bool, float, int, str}
            Names and values of all attributes.

        """
        self._refresh_cache()
        return _utils.unravel_key_values(self._msg.attributes)

    def del_attribute(self, key):
        """
        Deletes the attribute with name `key` from this dataset version.

        This method will not raise an error if the attribute does not exist.

        Parameters
        ----------
        key : str
            Name of the attribute.

        """
        _utils.validate_flat_key(key)

        # build KeyValues
        Message = _DatasetVersionService.DeleteDatasetVersionAttributes
        msg = Message(id=self.id, attribute_keys=[key])
        endpoint = "/api/v1/modeldb/dataset-version/deleteDatasetVersionAttributes"
        self._update(msg, Message.Response, endpoint, "DELETE")

    def _update(self, msg, response_proto, endpoint, method):
        response = self._conn.make_proto_request(method, endpoint, body=msg)
        self._conn.must_proto_response(response, response_proto)
        self._clear_cache()

    def delete(self):
        """
        Deletes this dataset version.

        """
        msg = _DatasetVersionService.DeleteDatasetVersion(id=self.id)
        response = self._conn.make_proto_request(
            "DELETE", "/api/v1/modeldb/dataset-version/deleteDatasetVersion", body=msg,
        )
        self._conn.must_response(response)

    # The following properties are holdovers for backwards compatibility
    @property
    def desc(self):
        warnings.warn(
            "this attribute is deprecated and will be removed in an upcoming version;"
            " consider using `get_description()` instead",
            category=FutureWarning,
        )
        return self.get_description()

    @property
    def attrs(self):
        warnings.warn(
            "this attribute is deprecated and will be removed in an upcoming version;"
            " consider using `get_attributes()` instead",
            category=FutureWarning,
        )
        return self.get_attributes()

    @property
    def tags(self):
        warnings.warn(
            "this attribute is deprecated and will be removed in an upcoming version;"
            " consider using `get_tags()` instead",
            category=FutureWarning,
        )
        return self.get_tags()

    @property
    def _dataset_type(self):
        warnings.warn(
            "this attribute is deprecated and will be removed in an upcoming version;"
            " consider checking `get_content()` instead",
            category=FutureWarning,
        )
        # enum value for PATH, which is currently the only supported type
        # https://github.com/VertaAI/modeldb/blob/af5e3a7/protos/protos/public/modeldb/DatasetService.proto#L32
        return 1

    @property
    def dataset_version(self):
        warnings.warn(
            "this attribute is deprecated and will be removed in an upcoming version;"
            " interfacing directly with the internal protobuf structure is not supported",
            category=FutureWarning,
        )
        self._refresh_cache()
        return self._msg

    @property
    def dataset_version_info(self):
        warnings.warn(
            "this attribute is deprecated and will be removed in an upcoming version;"
            " consider using `get_content()` instead",
            category=FutureWarning,
        )
        self._refresh_cache()
        version_info_oneof = self._msg.WhichOneof('dataset_version_info')
        return getattr(self._msg, version_info_oneof)

    @property
    def base_path(self):
        raise AttributeError(
            "this attribute is no longer supported;"
            " considering accessing the paths of specific components"
            " using `list_components()[i].path` instead",
        )

    # The following properties are for managed
    # TODO: consolidate this with similar method in `_ModelDBEntity`
    def _get_url_for_artifact(self, dataset_component_path, method, part_num=0):
        """
        Obtains a URL to use for accessing stored artifacts.

        Parameters
        ----------
        dataset_component_path : str
            Filepath in dataset component blob.
        method : {'GET', 'PUT'}
            HTTP method to request for the generated URL.
        part_num : int, optional
            If using Multipart Upload, number of part to be uploaded.

        Returns
        -------
        response_msg : `_DatasetVersionService.GetUrlForDatasetBlobVersioned.Response`
            Backend response.

        """
        if method.upper() not in ("GET", "PUT"):
            raise ValueError("`method` must be one of {'GET', 'PUT'}")

        Message = _DatasetVersionService.GetUrlForDatasetBlobVersioned
        msg = Message(
            path_dataset_component_blob_path=dataset_component_path,
            method=method,
            part_number=part_num,
        )
        data = _utils.proto_to_json(msg)
        endpoint = "{}://{}/api/v1/modeldb/dataset-version/dataset/{}/datasetVersion/{}/getUrlForDatasetBlobVersioned".format(
            self._conn.scheme,
            self._conn.socket,
            self.dataset_id,
            self.id,
        )
        response = _utils.make_request("POST", endpoint, self._conn, json=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(response.json(), Message.Response)

        url = response_msg.url
        # accommodate port-forwarded NFS store
        if 'https://localhost' in url[:20]:
            url = 'http' + url[5:]
        if 'localhost%3a' in url[:20]:
            url = url.replace('localhost%3a', 'localhost:')
        if 'localhost%3A' in url[:20]:
            url = url.replace('localhost%3A', 'localhost:')
        response_msg.url = url

        return response_msg

    # TODO: consolidate this with similar method in `Commit`
    def _upload_artifact(self, dataset_component_path, file_handle, part_size=_artifact_utils._64MB):
        """
        Uploads `file_handle` to ModelDB artifact store.

        Parameters
        ----------
        dataset_component_path : str
            Filepath in dataset component blob.
        file_handle : file-like
            Artifact to be uploaded.
        part_size : int, default 64 MB
            If using multipart upload, number of bytes to upload per part.

        """
        file_handle.seek(0)

        # check if multipart upload ok
        url_for_artifact = self._get_url_for_artifact(dataset_component_path, "PUT", part_num=1)

        print("uploading {} to ModelDB".format(dataset_component_path))
        if url_for_artifact.multipart_upload_ok:
            # TODO: parallelize this
            file_parts = iter(lambda: file_handle.read(part_size), b'')
            for part_num, file_part in enumerate(file_parts, start=1):
                print("uploading part {}".format(part_num), end='\r')

                # get presigned URL
                url = self._get_url_for_artifact(dataset_component_path, "PUT", part_num=part_num).url

                # wrap file part into bytestream to avoid OverflowError
                #     Passing a bytestring >2 GB (num bytes > max val of int32) directly to
                #     ``requests`` will overwhelm CPython's SSL lib when it tries to sign the
                #     payload. But passing a buffered bytestream instead of the raw bytestring
                #     indicates to ``requests`` that it should perform a streaming upload via
                #     HTTP/1.1 chunked transfer encoding and avoid this issue.
                #     https://github.com/psf/requests/issues/2717
                part_stream = six.BytesIO(file_part)

                # upload part
                response = _utils.make_request("PUT", url, self._conn, data=part_stream)
                _utils.raise_for_http_error(response)

                # commit part
                url = "{}://{}/api/v1/modeldb/dataset-version/commitVersionedDatasetBlobArtifactPart".format(
                    self._conn.scheme,
                    self._conn.socket,
                )
                msg = _DatasetVersionService.CommitVersionedDatasetBlobArtifactPart(
                    dataset_version_id=self.id,
                    path_dataset_component_blob_path=dataset_component_path,
                )
                msg.artifact_part.part_number = part_num
                msg.artifact_part.etag = response.headers['ETag']
                data = _utils.proto_to_json(msg)
                response = _utils.make_request("POST", url, self._conn, json=data)
                _utils.raise_for_http_error(response)
            print()

            # complete upload
            url = "{}://{}/api/v1/modeldb/dataset-version/commitMultipartVersionedDatasetBlobArtifact".format(
                self._conn.scheme,
                self._conn.socket,
            )
            msg = _DatasetVersionService.CommitMultipartVersionedDatasetBlobArtifact(
                dataset_version_id=self.id,
                path_dataset_component_blob_path=dataset_component_path,
            )
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("POST", url, self._conn, json=data)
            _utils.raise_for_http_error(response)
        else:
            # upload full artifact
            if url_for_artifact.fields:
                # if fields were returned by backend, make a POST request and supply them as form fields
                response = _utils.make_request(
                    "POST", url_for_artifact.url, self._conn,
                    # requests uses the `files` parameter for sending multipart/form-data POSTs.
                    #     https://stackoverflow.com/a/12385661/8651995
                    # the file contents must be the final form field
                    #     https://docs.aws.amazon.com/AmazonS3/latest/dev/HTTPPOSTForms.html#HTTPPOSTFormFields
                    files=list(url_for_artifact.fields.items()) + [('file', file_handle)],
                )
            else:
                response = _utils.make_request("PUT", url_for_artifact.url, self._conn, data=file_handle)
            _utils.raise_for_http_error(response)

        print("upload complete")
