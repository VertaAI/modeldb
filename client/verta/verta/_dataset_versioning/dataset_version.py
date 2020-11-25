# -*- coding: utf-8 -*-

from __future__ import print_function

from ..external import six

from .._protos.public.common import CommonService_pb2 as _CommonCommonService
from .._protos.public.modeldb import DatasetVersionService_pb2 as _DatasetVersionService
from .._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService

from .._tracking import entity
from .._repository import commit
from .._internal_utils import (
    _utils,
)


class DatasetVersion(entity._ModelDBEntity):
    """
    Object representing a ModelDB dataset version.

    .. versionchanged:: 0.15.10
        The dataset versioning interface was updated for flexibility,
        robustness, and consistency with other ModelDB entities.

    This class provides read/write functionality for dataset version metadata and access to its content.

    There should not be a need to instantiate this class directly; please use
    :meth:`Dataset.create_version() <verta._dataset_versioning.dataset.Dataset.create_version>`.

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
        `dataset blob subclass <versioning.html#dataset>`__
            Dataset content.

        """
        self._refresh_cache()

        # create wrapper blob msg so we can reuse the repository system's proto-to-obj
        blob = _VersioningService.Blob()
        blob.dataset.CopyFrom(self._msg.dataset_blob)
        return commit.blob_msg_to_object(blob)

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
