# -*- coding: utf-8 -*-

from __future__ import print_function

import warnings

from verta.external import six

from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta._protos.public.modeldb import DatasetService_pb2 as _DatasetService

from verta.tracking.entities import _entity
from verta._internal_utils import (
    _utils,
)

from ._dataset_version import DatasetVersion
from ._dataset_versions import DatasetVersions


class Dataset(_entity._ModelDBEntity):
    """
    Object representing a ModelDB dataset.

    .. versionchanged:: 0.16.0
        The dataset versioning interface was updated for flexibility,
        robustness, and consistency with other ModelDB entities.

    This class provides read/write functionality for dataset metadata and access to its versions.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.create_dataset() <verta.Client.create_dataset>`.

    Attributes
    ----------
    id : str
        ID of this dataset.
    name : str
        Name of this dataset.
    workspace : str
        Workspace containing this dataset.
    versions : :class:`~verta.dataset.entities.DatasetVersions`
        Versions of this dataset.

    """
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
    def workspace(self):
        self._refresh_cache()

        if self._msg.workspace_id:
            return self._conn.get_workspace_name_from_legacy_id(self._msg.workspace_id)
        else:
            return self._conn._OSS_DEFAULT_WORKSPACE

    @property
    def versions(self):
        return DatasetVersions(self._conn, self._conf).with_dataset(self)

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
    def _create_proto_internal(cls, conn, ctx, name, desc=None, tags=None, attrs=None, time_created=None, public_within_org=None, visibility=None):
        Message = _DatasetService.CreateDataset
        msg = Message(name=name, description=desc, tags=tags, attributes=attrs, workspace_name=ctx.workspace_name, time_created=time_created)
        if (public_within_org
                and ctx.workspace_name is not None  # not user's personal workspace
                and _utils.is_org(ctx.workspace_name, conn)):  # not anyone's personal workspace
            msg.dataset_visibility = _DatasetService.DatasetVisibilityEnum.ORG_SCOPED_PUBLIC
        msg.custom_permission.CopyFrom(visibility._custom_permission)
        msg.visibility = visibility._visibility

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
        Sets the description of this dataset.

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
        Gets the description of this dataset.

        Returns
        -------
        str
            Description of this dataset.

        """
        self._refresh_cache()
        return self._msg.description

    def add_tag(self, tag):
        """
        Adds a tag to this dataset.

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
        Adds multiple tags to this dataset.

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
        Gets all tags from this dataset.

        Returns
        -------
        list of str
            All tags.

        """
        self._refresh_cache()
        return self._msg.tags

    def del_tag(self, tag):
        """
        Deletes a tag from this dataset.

        This method will not raise an error if the tag does not exist.

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
        Adds an attribute to this dataset.

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
        Adds potentially multiple attributes to this dataset.

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
        Gets the attribute with name `key` from this dataset.

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
        Gets all attributes from this dataset.

        Returns
        -------
        dict of str to {None, bool, float, int, str}
            Names and values of all attributes.

        """
        self._refresh_cache()
        return _utils.unravel_key_values(self._msg.attributes)

    def del_attribute(self, key):
        """
        Deletes the attribute with name `key` from this dataset.

        This method will not raise an error if the attribute does not exist.

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

    def create_version(self, content, desc=None, tags=None, attrs=None, date_created=None):  # TODO: enable_mdb_versioning
        """
        Creates a dataset version.

        Parameters
        ----------
        content : `dataset blob subclass <versioning.html#dataset>`__
            Dataset content.
        desc : str, optional
            Description of the dataset version.
        tags : list of str, optional
            Tags of the dataset version.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the dataset version.

        Returns
        -------
        :class:`~verta.dataset.entities.DatasetVersion`

        Examples
        --------
        .. code-block:: python

            from verta.dataset import Path
            version = dataset.create_version(Path("data.csv"))

        """
        return DatasetVersion._create(
            self._conn, self._conf,
            dataset=self, dataset_blob=content,
            desc=desc, tags=tags, attrs=attrs,
            time_logged=date_created, time_updated=date_created,
        )

    def get_version(self, id):
        """
        Gets the specified dataset version.

        Parameters
        ----------
        id : str
            Dataset version ID.

        Returns
        -------
        :class:`~verta.dataset.entities.DatasetVersion`

        """
        return DatasetVersion._get_by_id(self._conn, self._conf, id)

    def get_latest_version(self):
        """
        Gets the latest dataset version.

        Returns
        -------
        :class:`~verta.dataset.entities.DatasetVersion`

        """
        return DatasetVersion._get_latest_version_by_dataset_id(self._conn, self._conf, self.id)

    def _update(self, msg, response_proto, endpoint, method):
        response = self._conn.make_proto_request(method, endpoint, body=msg)
        self._conn.must_proto_response(response, response_proto)
        self._clear_cache()

    def delete(self):
        """
        Deletes this dataset.

        """
        msg = _DatasetService.DeleteDataset(id=self.id)
        response = self._conn.make_proto_request(
            "DELETE", "/api/v1/modeldb/dataset/deleteDataset", body=msg,
        )
        self._conn.must_response(response)

    # The following properties are holdovers for backwards compatibility
    @property
    def dataset_type(self):
        raise AttributeError(
            "this attribute is no longer supported;"
            " datasets no longer have a specific associated type",
        )

    @property
    def _dataset_type(self):
        raise AttributeError(
            "this attribute is no longer supported;"
            " datasets no longer have a specific associated type",
        )

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
