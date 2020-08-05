# -*- coding: utf-8 -*-

from __future__ import print_function

from ..external import six

from .._protos.public.modeldb import DatasetService_pb2 as _DatasetService
from .._protos.public.modeldb import CommonService_pb2 as _CommonService

from .._tracking import entity
from .._internal_utils import (
    _utils,
)
from .. import dataset

from .dataset_version import DatasetVersion


class Dataset(entity._ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(Dataset, self).__init__(conn, conf, _DatasetService, "dataset", msg)

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg

        return '\n'.join((
            "name: {}".format(msg.name),
            "url: {}://{}/{}/datasets/{}/summary".format(self._conn.scheme, self._conn.socket, self.workspace, self.id),
            "time created: {}".format(_utils.timestamp_to_str(int(msg.time_created))),
            "time updated: {}".format(_utils.timestamp_to_str(int(msg.time_updated))),
            "description: {}".format(msg.description),
            "tags: {}".format(msg.tags),
            "attributes: {}".format(_utils.unravel_key_values(msg.attributes)),
            "id: {}".format(msg.id),
        ))

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name

    @property
    def dataset_type(self):
        return self.__class__.__name__

    @property
    def workspace(self):
        self._refresh_cache()

        if self._msg.workspace_id:
            return self._get_workspace_name_by_id(self._msg.workspace_id)
        else:
            return entity._OSS_DEFAULT_WORKSPACE

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
        """
        Sets the description of this Dataset.

        Parameters
        ----------
        desc : str
            Description to set.

        """
        Message = _DatasetService.UpdateDatasetDescription
        msg = Message(id=self.id, description=desc)
        endpoint = "/api/v1/modeldb/dataset/updateDatasetDescription"
        self._update(msg, Message.Response, endpoint, "POST")

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
        """
        Adds a tag to this Dataset.

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
        Adds multiple tags to this Dataset.

        Parameters
        ----------
        tags : list of str
            Tags to add.

        """
        tags = _utils.as_list_of_str(tags)
        Message = _DatasetService.AddDatasetTags
        msg = Message(id=self.id, tags=tags)
        endpoint = "/api/v1/modeldb/dataset/addDatasetTags"
        self._update(msg, Message.Response, endpoint, "POST")

    def get_tags(self):
        """
        Gets all tags from this Dataset.

        Returns
        -------
        list of str
            All tags.

        """
        self._refresh_cache()
        return self._msg.tags

    def del_tag(self, tag):
        """
        Deletes a tag from this Dataset.

        Parameters
        ----------
        tag : str
            Tag to delete.

        """
        if not isinstance(tag, six.string_types):
            raise TypeError("`tag` must be a string")

        Message = _DatasetService.DeleteDatasetTags
        msg = Message(id=self.id, tags=[tag])
        endpoint = "/api/v1/modeldb/dataset/deleteDatasetTags"
        self._update(msg, Message.Response, endpoint, "DELETE")

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

    def create_s3_version(self, paths, desc=None, tags=None, attrs=None, date_created=None):  # TODO: enable_mdb_versioning
        raise NotImplementedError

    def create_path_version(self, paths, base_path=None, desc=None, tags=None, attrs=None, date_created=None):  # TODO: enable_mdb_versioning
        dataset_blob = dataset.Path(paths=paths, base_path=base_path)
        return DatasetVersion._create(
            self._conn, self._conf,
            dataset=self, dataset_blob=dataset_blob,
            desc=desc, tags=tags, attrs=attrs,
            time_logged=date_created, time_updated=date_created,
        )

    def get_version(self, id):
        raise NotImplementedError

    def get_latest_version(self):
        raise NotImplementedError

    def _update(self, msg, response_proto, endpoint, method):
        response = self._conn.make_proto_request(method, endpoint, body=msg)
        self._conn.must_proto_response(response, response_proto)
        self._clear_cache()
