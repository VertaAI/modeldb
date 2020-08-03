# -*- coding: utf-8 -*-

from __future__ import print_function

from .._tracking import entity

from .._protos.public.modeldb import DatasetService_pb2 as _DatasetService
from .._protos.public.common import CommonService_pb2 as _CommonCommonService

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
        Message = _DatasetService.GetDatasetById
        msg = Message(id=id)
        endpoint = "/api/v1/modeldb/dataset/getDatasetById"
        response = conn.make_proto_request("GET", endpoint, params=msg)
        return conn.maybe_proto_response(response, Message.Response).dataset

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        Message = _DatasetService.GetDatasetByName
        msg = Message(name=name, workspace_name=workspace)
        endpoint = "/api/v1/modeldb/dataset/getDatasetByName"
        proto_response = conn.make_proto_request("GET", endpoint, params=msg)
        response = conn.maybe_proto_response(proto_response, Message.Response)

        if workspace is None or response.HasField("dataset_by_user"):
            return response.dataset_by_user
        elif hasattr(response, "shared_datasets") and response.shared_datasets:
            return response.shared_datasets[0]
        else:
            return None

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, desc=None, tags=None, attrs=None, time_created=None, public_within_org=None):
        Message = _DatasetService.CreateDataset
        msg = Message(name=name, description=desc, tags=tags, attributes=attrs, workspace_name=ctx.workspace_name, time_created=time_created)

        if public_within_org:
            if ctx.workspace_name is None:
                raise ValueError("cannot set `public_within_org` for personal workspace")
            elif not _utils.is_org(ctx.workspace_name, conn):
                raise ValueError(
                    "cannot set `public_within_org`"
                    " because workspace \"{}\" is not an organization".format(ctx.workspace_name)
                )
            else:
                msg.project_visibility = _DatasetService.DatasetVisibilityEnum.ORG_SCOPED_PUBLIC

        endpoint = "/api/v1/modeldb/dataset/createDataset"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        dataset = conn.must_proto_response(response, Message.Response).dataset

        if ctx.workspace_name is not None:
            WORKSPACE_PRINT_MSG = "workspace: {}".format(ctx.workspace_name)
        else:
            WORKSPACE_PRINT_MSG = "personal workspace"

        print("created new Dataset: {} in {}".format(dataset.name, WORKSPACE_PRINT_MSG))
        return dataset

    def set_description(self, desc):
        raise NotImplementedError

    def get_description(self):
        raise NotImplementedError

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
        """
        Adds an attribute to this Dataset.

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
        Adds potentially multiple attributes to this Dataset.

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
            attribute_keyvals.append(_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value, allow_collection=True)))

        Message = _DatasetService.AddDatasetAttributes
        msg = Message(id=self.id, attributes=attribute_keyvals)
        endpoint = "/api/v1/modeldb/dataset/addDatasetAttributes"
        self._update(msg, Message.Response, endpoint, "POST")

    def get_attribute(self, key):
        """
        Gets the attribute with name `key` from this Dataset.

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
        Gets all attributes from this Dataset.

        Returns
        -------
        dict of str to {None, bool, float, int, str}
            Names and values of all attributes.

        """
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
