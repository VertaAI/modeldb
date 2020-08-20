# -*- coding: utf-8 -*-

from __future__ import print_function

import abc

from ..external import six

from .._protos.public.modeldb import DatasetVersionService_pb2 as _DatasetVersionService

from .._tracking import entity
from .._internal_utils import (
    _utils,
)


@six.add_metaclass(abc.ABCMeta)
class DatasetVersion(entity._ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(DatasetVersion, self).__init__(conn, conf, _DatasetVersionService, "dataset-version", msg)

    def __repr__(self):
        raise NotImplementedError

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name

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
    def _get_latest_version_by_dataset_id(cls, conn, dataset_id):
        Message = _DatasetVersionService.GetLatestDatasetVersionByDatasetId
        msg = Message(dataset_id=dataset_id)
        endpoint = "/api/v1/modeldb/dataset-version/getLatestDatasetVersionByDatasetId"
        response = conn.make_proto_request("GET", endpoint, params=msg)
        return conn.must_proto_response(response, Message.Response).dataset_version

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

    def set_description(self, desc):
        raise NotImplementedError

    def get_description(self):
        raise NotImplementedError

    def add_tag(self, tag):
        """
        Adds a tag to this Dataset Version.

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
        Adds multiple tags to this Dataset Version.

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
        Gets all tags from this Dataset Version.

        Returns
        -------
        list of str
            All tags.

        """
        self._refresh_cache()
        return self._msg.tags

    def del_tag(self, tag):
        """
        Deletes a tag from this Dataset Version.

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
        raise NotImplementedError

    def add_attributes(self, attrs):
        raise NotImplementedError

    def get_attribute(self, key):
        raise NotImplementedError

    def get_attributes(self):
        raise NotImplementedError

    def del_attribute(self, key):
        raise NotImplementedError

    def _update(self, msg, response_proto, endpoint, method):
        response = self._conn.make_proto_request(method, endpoint, body=msg)
        self._conn.must_proto_response(response, response_proto)
        self._clear_cache()
