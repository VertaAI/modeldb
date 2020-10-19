# -*- coding: utf-8 -*-

from __future__ import print_function

from ..external import six

from .._protos.public.common import CommonService_pb2 as _CommonCommonService
from .._protos.public.modeldb import CommonService_pb2 as _CommonService
from .._protos.public.modeldb import DatasetService_pb2 as _DatasetService

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

        if response.HasField("dataset_by_user") and response.dataset_by_user.id:
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
        self._refresh_cache()
        return _utils.unravel_key_values(self._msg.attributes)

    def del_attribute(self, key):
        """
        Deletes the attribute with name `key` from this Dataset

        Parameters
        ----------
        key : str
            Name of the attribute.

        """
        _utils.validate_flat_key(key)

        # build KeyValues
        Message = _DatasetService.DeleteDatasetAttributes
        msg = Message(id=self.id, attribute_keys=[key])
        endpoint = "/api/v1/modeldb/dataset/deleteDatasetAttributes"
        self._update(msg, Message.Response, endpoint, "DELETE")

    def create_s3_version(self, paths, desc=None, tags=None, attrs=None, date_created=None):  # TODO: enable_mdb_versioning
        """
        Creates s3 dataset version

        Parameters
        ----------
        paths : list of str
            Dataset version paths.
        desc : str, optional
            Description of the Dataset version.
        tags : list of str, optional
            Tags of the Dataset Version.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the Dataset Version.

        Returns
        -------
        `DatasetVersion <dataset.html>`_

        """
        dataset_blob = dataset.S3(paths=paths)
        return DatasetVersion._create(
            self._conn, self._conf,
            dataset=self, dataset_blob=dataset_blob,
            desc=desc, tags=tags, attrs=attrs,
            time_logged=date_created, time_updated=date_created,
        )

    def create_path_version(self, paths, base_path=None, desc=None, tags=None, attrs=None, date_created=None):
        """
        Creates path dataset version

        Parameters
        ----------
        paths : list of str
            Dataset version paths.
        desc : str, optional
            Description of the Dataset version.
        tags : list of str, optional
            Tags of the Dataset Version.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the Dataset Version.

        Returns
        -------
        `DatasetVersion <dataset.html>`_

        """
        dataset_blob = dataset.Path(paths=paths, base_path=base_path)
        return DatasetVersion._create(
            self._conn, self._conf,
            dataset=self, dataset_blob=dataset_blob,
            desc=desc, tags=tags, attrs=attrs,
            time_logged=date_created, time_updated=date_created,
        )

    def get_version(self, id):
        """
        Gets the specified dataset version.

        Parameters
        ----------
        id : str
            Dataset version id

        Returns
        -------
        `DatasetVersion <dataset.html>`_

        """
        return DatasetVersion._get_by_id(self._conn, self._conf, id)

    def get_latest_version(self):
        """
        Gets the latest dataset version.

        Returns
        -------
        `DatasetVersion <dataset.html>`_

        """
        return DatasetVersion._get_latest_version_by_dataset_id(self._conn, self.id)

    def _update(self, msg, response_proto, endpoint, method):
        response = self._conn.make_proto_request(method, endpoint, body=msg)
        self._conn.must_proto_response(response, response_proto)
        self._clear_cache()
