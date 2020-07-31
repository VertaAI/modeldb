# -*- coding: utf-8 -*-

from __future__ import print_function

from .._tracking import entity

from .._protos.public.modeldb import DatasetService_pb2 as _DatasetService

from ..external import six

from .._internal_utils import (
    _utils,
)


class Dataset(entity._ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(Dataset, self).__init__(conn, conf, _DatasetService, "dataset", msg)

    def __repr__(self):
        raise NotImplementedError

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name

    @property
    def dataset_type(self):
        return self.__class__.__name__

    @property
    def desc(self):
        # for backwards compatibility
        # TODO: deprecate
        return self.get_description()

    @property
    def attrs(self):
        # for backwards compatibility
        # TODO: deprecate
        return self.get_attributes()

    @property
    def tags(self):
        # for backwards compatibility
        # TODO: deprecate
        return self.get_tags()

    @property
    def versions(self):
        raise NotImplementedError

    @classmethod
    def _generate_default_name(cls):
        return "Dataset {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        raise NotImplementedError

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        raise NotImplementedError

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, desc=None, tags=None, attrs=None, date_created=None, public_within_org=None):
        raise NotImplementedError

    def set_description(self, desc):
        """
        Sets the description of this Dataset.

        Parameters
        ----------
        desc : str
            Description to add.

        """
        self._fetch_with_no_cache()
        Message = _DatasetService.UpdateDatasetDescription
        msg = Message(id=self.id, description=desc)
        endpoint = "/api/v1/modeldb/dataset/updateDatasetDescription"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        self._msg = self._conn.must_proto_response(response, Message.Response).dataset

    def get_description(self):
        """
        Gets the description of this Dataset.

        Returns
        -------
        str
            Description of this Dataset.

        """
        self._refresh_cache()
        return self._msg.description

    def add_tag(self, tag):
        raise NotImplementedError

    def add_tags(self, tags):
        """
        Parameters
        ----------
        tags : list of str

        """
        raise NotImplementedError

    def get_tags(self):
        raise NotImplementedError

    def del_tag(self, tag):
        raise NotImplementedError

    def add_attribute(self, key, value):
        raise NotImplementedError

    def add_attributes(self, attrs):
        """
        Parameters
        ----------
        attrs : dict of str to any

        """
        raise NotImplementedError

    def get_attribute(self, key):
        raise NotImplementedError

    def get_attributes(self):
        raise NotImplementedError

    def del_attribute(self, key):
        raise NotImplementedError

    def create_s3_version(self):  # TODO: same params as S3.__init__()
        raise NotImplementedError
        # TODO: create S3 blob and pass as self._create(dataset_blob=blob)

    def create_path_version(self):  # TODO: same params as Path.__init__()
        raise NotImplementedError

    def get_latest_version(self):
        raise NotImplementedError
