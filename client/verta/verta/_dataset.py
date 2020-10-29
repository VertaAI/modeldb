# -*- coding: utf-8 -*-

from __future__ import print_function

import hashlib
import os
import time
import warnings

import requests

from ._protos.public.common import CommonService_pb2 as _CommonCommonService
from ._protos.public.modeldb import DatasetService_pb2 as _DatasetService
from ._protos.public.modeldb import DatasetVersionService_pb2 as _DatasetVersionService

from .external import six

from ._internal_utils import (
    _utils,
    importer,
)

from ._dataset_versioning.dataset_versions import DatasetVersions


class Dataset(object):
    # TODO: delete is not supported on the API yet
    def __init__(self, conn, conf,
                 name=None, dataset_type=None,
                 desc=None, tags=None, attrs=None,
                 workspace=None,
                 public_within_org=None,
                 _dataset_id=None):
        if name is not None and _dataset_id is not None:
            raise ValueError("cannot specify both `name` and `_dataset_id`")

        if workspace is not None:
            WORKSPACE_PRINT_MSG = "workspace: {}".format(workspace)
        else:
            WORKSPACE_PRINT_MSG = "personal workspace"

        if _dataset_id is not None:
            dataset = Dataset._get(conn, _dataset_id=_dataset_id)
            if dataset is not None:
                print("set existing Dataset: {}".format(dataset.name))
            else:
                raise ValueError("Dataset with ID {} not found".format(_dataset_id))
        else:
            if name is None:
                name = Dataset._generate_default_name()
            try:
                dataset = Dataset._create(conn, name, dataset_type, desc, tags, attrs, workspace, public_within_org)
            except requests.HTTPError as e:
                if e.response.status_code == 403:  # cannot create in other workspace
                    dataset = Dataset._get(conn, name, workspace)
                    if dataset is not None:
                        print("set existing Dataset: {} from {}".format(dataset.name, WORKSPACE_PRINT_MSG))
                    else:  # no accessible dataset in other workspace
                        six.raise_from(e, None)
                elif e.response.status_code == 409:  # already exists
                    if any(param is not None for param in (desc, tags, attrs, public_within_org)):
                        warnings.warn(
                            "Dataset with name {} already exists;"
                            " cannot set `desc`, `tags`, `attrs`, or `public_within_org`".format(name)
                        )
                    dataset = Dataset._get(conn, name, workspace)
                    if dataset is not None:
                        print("set existing Dataset: {} from {}".format(dataset.name, WORKSPACE_PRINT_MSG))
                    else:
                        raise RuntimeError("unable to retrieve Dataset {};"
                                           " please notify the Verta development team".format(name))
                else:
                    raise e
            else:
                print("created new Dataset: {} in {}".format(dataset.name, WORKSPACE_PRINT_MSG))

        # this is available to create versions
        self._conn = conn
        self._conf = conf

        self.id = dataset.id

        # these could be updated by separate calls
        self.name = dataset.name
        self.dataset_type = self.__class__.__name__
        self._dataset_type = dataset.dataset_type
        self.desc = dataset.description
        self.attrs = dataset.attributes
        self.tags = dataset.tags

    def __repr__(self):
        return "<{} \"{}\">".format(self.__class__.__name__, self.name)

    @staticmethod
    def _generate_default_name():
        return "Dataset {}".format(_utils.generate_default_name())

    @staticmethod
    def _get(conn, dataset_name=None, workspace=None, _dataset_id=None):
        if _dataset_id is not None:
            Message = _DatasetService.GetDatasetById
            msg = Message(id=_dataset_id)
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("GET",
                                           "{}://{}/api/v1/modeldb/dataset/getDatasetById".format(conn.scheme, conn.socket),
                                           conn, params=data)

            if response.ok:
                dataset = _utils.json_to_proto(_utils.body_to_json(response), Message.Response).dataset
                return dataset
            else:
                if response.status_code == 404 and _utils.body_to_json(response)['code'] == 5:
                    return None
                else:
                    _utils.raise_for_http_error(response)
        elif dataset_name is not None:
            Message = _DatasetService.GetDatasetByName
            msg = Message(name=dataset_name, workspace_name=workspace)
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("GET",
                                           "{}://{}/api/v1/modeldb/dataset/getDatasetByName".format(conn.scheme, conn.socket),
                                           conn, params=data)

            if response.ok:
                response_json = _utils.body_to_json(response)
                response_msg = _utils.json_to_proto(response_json, Message.Response)
                if workspace is None or response_json.get('dataset_by_user'):
                    # user's personal workspace
                    dataset = response_msg.dataset_by_user
                else:
                    dataset = response_msg.shared_datasets[0]

                if not dataset.id:  # 200, but empty message
                    raise RuntimeError("unable to retrieve Dataset {};"
                                       " please notify the Verta development team".format(dataset_name))

                return dataset
            else:
                if response.status_code == 404 and _utils.body_to_json(response)['code'] == 5:
                    return None
                else:
                    _utils.raise_for_http_error(response)
        else:
            raise ValueError("insufficient arguments")

    @staticmethod
    def _create(conn, dataset_name, dataset_type, desc=None, tags=None, attrs=None, workspace=None, public_within_org=None):
        if tags is not None:
            tags = _utils.as_list_of_str(tags)
        if attrs is not None:
            attrs = [_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value, allow_collection=True))
                     for key, value in six.viewitems(attrs)]

        Message = _DatasetService.CreateDataset
        msg = Message(name=dataset_name, dataset_type=dataset_type,
                      description=desc, tags=tags, attributes=attrs,
                      workspace_name=workspace)
        if public_within_org:
            if workspace is None:
                raise ValueError("cannot set `public_within_org` for personal workspace")
            elif not _utils.is_org(workspace, conn):
                raise ValueError(
                    "cannot set `public_within_org`"
                    " because workspace \"{}\" is not an organization".format(workspace)
                )
            else:
                msg.dataset_visibility = _DatasetService.DatasetVisibilityEnum.ORG_SCOPED_PUBLIC
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/dataset/createDataset".format(conn.scheme, conn.socket),
                                       conn, json=data)

        if response.ok:
            dataset = _utils.json_to_proto(_utils.body_to_json(response), Message.Response).dataset
            return dataset
        else:
            _utils.raise_for_http_error(response)

    def create_version(self):
        raise NotImplementedError('this function must be implemented by subclasses')

    def get_all_versions(self):
        """
        Gets all the versions for this Dataset.

        Returns
        -------
        list of DatasetVersions for this Dataset

        """
        return DatasetVersions(self._conn, self._conf).with_dataset(self)

    # TODO: sorting seems to be incorrect
    def get_latest_version(self, ascending=None, sort_key=None):
        """
        Gets the latest version for this Dataset.

        Parameters
        ----------
        ascending : bool
            Whether to sort by time in ascending order

        sort_key : str
            Which key to sort on

        Returns
        -------
        Returns the latest version of the Dataset as defined by ascending and the
        sort_key.

        """
        Message = _DatasetVersionService.GetLatestDatasetVersionByDatasetId
        msg = Message(dataset_id=self.id, ascending=ascending, sort_key=sort_key)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/dataset-version/getLatestDatasetVersionByDatasetId".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        print("got existing dataset version: {}".format(response_msg.dataset_version.id))
        return DatasetVersion(self._conn, self._conf, _dataset_version_id=response_msg.dataset_version.id)


class RawDataset(Dataset):
    TYPE = _DatasetService.DatasetTypeEnum.RAW

    def __init__(self, *args, **kwargs):
        super(RawDataset, self).__init__(dataset_type=self.TYPE, *args, **kwargs)


class PathDataset(Dataset):
    TYPE = _DatasetService.DatasetTypeEnum.PATH

    def __init__(self, *args, **kwargs):
        super(PathDataset, self).__init__(dataset_type=self.TYPE, *args, **kwargs)


class QueryDataset(Dataset):
    TYPE = _DatasetService.DatasetTypeEnum.QUERY

    def __init__(self, *args, **kwargs):
        super(QueryDataset, self).__init__(dataset_type=self.TYPE, *args, **kwargs)


class S3Dataset(PathDataset):
    def create_version(self,
                       bucket_name, key=None, key_prefix=None,
                       url_stub=None,
                       parent_id=None,
                       desc=None, tags=None, attrs=None):
        """
        Create a version of an S3 dataset.

        Parameters
        ----------
        bucket_name : str
            Name of the S3 bucketing storing the data.
        key : str, optional
            Key of the object in S3. This argument cannot be provided alongside `key_prefix`.
        key_prefix : str, optional
            Key prefix to use for snapshotting multiple objects. This argument cannot be provided
            alongside `key`.
        url_stub : str, optional
            Prefix of the S3 URL.
        parent_id : str
            Id of the DatasetVersion from which this version was derived.
        desc : str, optional
            Description of the DatasetVersion.
        tags : list of str, optional
            Tags of the DatasetVersion.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the DatasetVersion.

        Returns
        -------
        :class:`~verta._dataset.DatasetVersion`
            Returns the newly created dataset version
        """
        if key is not None and key_prefix is not None:
            raise ValueError("cannot specify both `key` and `key_prefix`")

        version_info = S3DatasetVersionInfo(bucket_name, key=key, key_prefix=key_prefix, url_stub=url_stub)
        return PathDatasetVersion(self._conn, self._conf,
                                  dataset_id=self.id, dataset_type=self.TYPE,
                                  dataset_version_info=version_info,
                                  parent_id=parent_id,
                                  desc=desc, tags=tags, attrs=attrs)


class LocalDataset(PathDataset):
    def create_version(self,
                       path,
                       parent_id=None,
                       desc=None, tags=None, attrs=None):
        """
        Create a version of a Local dataset.

        Parameters
        ----------
        path : str
            Path to the local dataset.
        parent_id : str
            Id of the DatasetVersion from which this version was derived.
        desc : str, optional
            Description of the DatasetVersion.
        tags : list of str, optional
            Tags of the DatasetVersion.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the DatasetVersion.

        Returns
        -------
        :class:`~verta._dataset.DatasetVersion`
            Returns the newly created dataset version
        """
        version_info = FilesystemDatasetVersionInfo(path)
        return PathDatasetVersion(self._conn, self._conf,
                                  dataset_id=self.id, dataset_type=self.TYPE,
                                  dataset_version_info=version_info,
                                  parent_id=parent_id,
                                  desc=desc, tags=tags, attrs=attrs)


class BigQueryDataset(QueryDataset):
    def create_version(self,
                       job_id, location,
                       parent_id=None,
                       desc=None, tags=None, attrs=None):
        """
        Create a version of a Big Query dataset.

        Parameters
        ----------
        job_id : str
            Id of the Big Query job that created this Dataset.
        location : str
            Big Query location parameter.
        parent_id : str
            Id of the DatasetVersion from which this version was derived.
        desc : str, optional
            Description of the DatasetVersion.
        tags : list of str, optional
            Tags of the DatasetVersion.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the DatasetVersion.

        Returns
        -------
        :class:`~verta._dataset.DatasetVersion`
            Returns the newly created dataset version
        """
        version_info = BigQueryDatasetVersionInfo(job_id=job_id, location=location)
        return QueryDatasetVersion(self._conn, self._conf,
                                   dataset_id=self.id, dataset_type=self.TYPE,
                                   dataset_version_info=version_info,
                                   parent_id=parent_id,
                                   desc=desc, tags=tags, attrs=attrs)

class RDBMSDataset(QueryDataset):
    def create_version(self,
                       query, db_connection_str,
                       execution_timestamp=None, query_template="",
                       query_parameters=[], num_records=0,
                       parent_id=None,
                       desc=None, tags=None, attrs=None):
        """
        Create a version of a generic RDBMS Query Dataset.

        Parameters
        ----------
        query : str
            Query that was executed.
        db_connection_str : str
            Connection string for the DBMS (e.g., localhost:8080).
        execution_timestamp : int or float, optional
            Time at which the query was run. Will default to current time.
        query_template : str, optional
            Template from which the query was derived.
        query_parameters : list of str, optional
            Parameters used with the query template.
        num_records : int, optional
            Number of records returned by the query.
        parent_id : str, optional
            Id of the DatasetVersion from which this version was derived.
        desc : str, optional
            Description of the DatasetVersion.
        tags : list of str, optional
            Tags of the DatasetVersion.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the DatasetVersion.

        Returns
        -------
        :class:`~verta._dataset.DatasetVersion`
            Returns the newly created dataset version
        """
        version_info = RDBMSDatasetVersionInfo(
            query=query,
            db_connection_str=db_connection_str,
            execution_timestamp=execution_timestamp,
            query_template=query_template,
            query_parameters=query_parameters,
            num_records=num_records)
        return QueryDatasetVersion(self._conn, self._conf,
                                   dataset_id=self.id, dataset_type=self.TYPE,
                                   dataset_version_info=version_info,
                                   parent_id=parent_id,
                                   desc=desc, tags=tags, attrs=attrs)

class AtlasHiveDataset(QueryDataset):
    def create_version(self,
                       guid, atlas_url,
                       atlas_user_name, atlas_password,
                       atlas_entity_endpoint="/api/atlas/v2/entity/bulk",
                       parent_id=None,
                       desc=None, tags=None, attrs=None):
        """
        Create a version of an AtlasHive dataset.

        Parameters
        ----------
        guid : str
            GUID of the table being queried.
        atlas_url : str
            Path to the local dataset.
        atlas_user_name : str
            Username for Atlas
        atlas_password : str
            Password for Atlas
        atlas_entity_endpoint : str
            Endpoint for querying Atlas
        parent_id : str
            Id of the DatasetVersion from which this version was derived.
        desc : str, optional
            Description of the DatasetVersion.
        tags : list of str, optional
            Tags of the DatasetVersion.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the DatasetVersion.

        Returns
        -------
        `DatasetVersion <dataset.html>`_
            Returns the newly created dataset version
        """
        version_info = AtlasHiveDatasetVersionInfo(
            guid=guid, atlas_url=atlas_url,
            atlas_user_name=atlas_user_name, atlas_password=atlas_password,
            atlas_entity_endpoint=atlas_entity_endpoint
        )
        return QueryDatasetVersion(self._conn, self._conf,
                                   dataset_id=self.id, dataset_type=self.TYPE,
                                   dataset_version_info=version_info, parent_id=parent_id,
                                   desc=desc,
                                   tags=tags or version_info.tags or [],
                                   attrs=attrs or version_info.attributes or {})


class DatasetVersion(object):
    """
    A version of a dataset at a particular point in time.

    Attributes
    ----------
    id : str
        ID of this dataset version.
    base_path : str
        Base path of this dataset version's components.

    """
    # TODO: visibility not done
    # TODO: delete version not implemented
    def __init__(self, conn, conf,
                 dataset_id=None, dataset_type=None,
                 dataset_version_info=None,
                 parent_id=None,
                 desc=None, tags=None, attrs=None,
                 version=None, _dataset_version_id=None):
        if _dataset_version_id is not None:
            # retrieve dataset version by id
            dataset_version = DatasetVersion._get(conn, _dataset_version_id)
            if dataset_version is None:
                raise ValueError("DatasetVersion with ID {} not found".format(_dataset_version_id))
        else:
            # create new version under dataset
            if dataset_id is None:
                raise ValueError('dataset_id must be specified')

            # create a new dataset version
            try:
                dataset_version = DatasetVersion._create(
                    conn,
                    dataset_id, dataset_type,
                    dataset_version_info,
                    parent_id=parent_id,
                    desc=desc, tags=tags, attrs=attrs,
                    version=version
                )

            # TODO: handle dups
            except requests.HTTPError as e:
                # if e.response.status_code == 409:  # already exists
                #     if any(param is not None for param in (desc, tags, attrs)):
                #         warnings.warn("Dataset with name {} already exists;"
                #                         " cannot initialize `desc`, `tags`, or `attrs`".format(dataset_name))
                #     dataset_version = DatasetVersion._get(conn, dataset_id, version)
                # else:
                #     raise e
                raise e
            else:
                print("created new DatasetVersion: {}"
                      .format(dataset_version.version))

        self._conn = conn
        self._conf = conf
        self.dataset_id = dataset_version.dataset_id

        # this info can be captured via a separate call too
        self.parent_id = dataset_version.parent_id
        self.desc = dataset_version.description
        self.tags = dataset_version.tags
        self.attrs = dataset_version.attributes
        self.id = dataset_version.id
        self.version = dataset_version.version
        self._dataset_type = dataset_version.dataset_type
        self.dataset_version = dataset_version

        version_info_oneof = dataset_version.WhichOneof('dataset_version_info')
        if version_info_oneof:
            self.dataset_version_info = getattr(dataset_version, version_info_oneof)
        else:
            self.dataset_version_info = None

        # assign base_path to proto msg to restore a level of backwards-compatibility
        try:
            self.dataset_version.path_dataset_version_info.base_path = self.base_path
        except AttributeError:  # unsupported non-path dataset or multiple base_paths
            pass

    def __repr__(self):
        if self.dataset_version:
            msg_copy = self.dataset_version.__class__()
            msg_copy.CopyFrom(self.dataset_version)
            msg_copy.owner = ''  # hide owner field
            return msg_copy.__repr__()
        elif self.dataset_version_info:
            msg_copy = self.dataset_version_info.__class__()
            msg_copy.CopyFrom(self.dataset_version_info)  # pylint: disable=no-member
            return msg_copy.__repr__()  # pylint: disable=no-member
        else:
            return "<{} \"{}\">".format(self.__class__.__name__, self.version)

    # TODO: get by dataset_id and version is not supported on the backend
    @staticmethod
    def _get(conn, _dataset_version_id=None):
        if _dataset_version_id is not None:
            Message = _DatasetVersionService.GetDatasetVersionById
            msg = Message(id=_dataset_version_id)
            data = _utils.proto_to_json(msg)
            response = _utils.make_request(
                "GET",
                "{}://{}/api/v1/modeldb/dataset-version/getDatasetVersionById".format(conn.scheme, conn.socket),
                conn, params=data
            )
            if response.ok:
                dataset_version = _utils.json_to_proto(_utils.body_to_json(response), Message.Response).dataset_version

                if not dataset_version.id:  # 200, but empty message
                    raise RuntimeError("unable to retrieve DatasetVersion {};"
                                       " please notify the Verta development team".format(_dataset_version_id))

                return dataset_version
            else:
                if response.status_code == 404 and _utils.body_to_json(response)['code'] == 5:
                    return None
                else:
                    _utils.raise_for_http_error(response)
        else:
            raise ValueError("insufficient arguments")

    @staticmethod
    def _create(conn,
                dataset_id, dataset_type,
                dataset_version_info,
                parent_id=None,
                desc=None, tags=None, attrs=None,
                version=None):
        if tags is not None:
            tags = _utils.as_list_of_str(tags)
        if attrs is not None:
            attrs = [_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value, allow_collection=True))
                     for key, value in six.viewitems(attrs)]

        if dataset_type == _DatasetService.DatasetTypeEnum.PATH:
            msg = PathDatasetVersion.make_create_message(
                dataset_id, dataset_type,
                dataset_version_info,
                parent_id=parent_id,
                desc=desc, tags=tags, attrs=attrs,
                version=version
            )
        elif dataset_type == _DatasetService.DatasetTypeEnum.QUERY:
            msg = QueryDatasetVersion.make_create_message(
                dataset_id, dataset_type,
                dataset_version_info,
                parent_id=parent_id,
                desc=desc, tags=tags, attrs=attrs,
                version=version
            )
        else:
            msg = RawDatasetVersion.make_create_message(
                dataset_id, dataset_type,
                dataset_version_info,
                parent_id=parent_id,
                desc=desc, tags=tags, attrs=attrs,
                version=version
            )

        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/dataset-version/createDatasetVersion".format(conn.scheme, conn.socket),
                                       conn, json=data)

        if response.ok:
            dataset_version = _utils.json_to_proto(_utils.body_to_json(response),
                                                   _DatasetVersionService.CreateDatasetVersion.Response).dataset_version
            return dataset_version
        else:
            _utils.raise_for_http_error(response)

    @staticmethod
    def make_create_message(dataset_id, dataset_type,
                            dataset_version_info,
                            parent_id=None,
                            desc=None, tags=None, attrs=None,
                            version=None):
        raise NotImplementedError('this function must be implemented by subclasses')

    @property
    def base_path(self):
        components = self.list_components()
        base_paths = set(component.base_path for component in components)

        if len(base_paths) == 1:
            return base_paths.pop()
        else:  # shouldn't happen: DVs don't have an interface to have different base paths
            raise AttributeError("multiple base paths among components: {}".format(base_paths))

    def list_components(self):
        """
        Returns a list of this dataset version's components.

        Returns
        -------
        list of :class:`~verta.dataset._dataset.Component`

        """
        # there's a circular import if imported at module-level
        # which I don't fully understand, and even breaks in Python 3
        # so this import is deferred to this function body
        from .dataset import _dataset

        blob = self.dataset_version.dataset_blob

        content_oneof = blob.WhichOneof('content')
        if content_oneof:
            # determine component type
            if content_oneof == "s3":
                component_cls = _dataset.S3Component
            elif content_oneof == "path":
                component_cls = _dataset.Component
            else:  # shouldn't happen
                raise RuntimeError(
                    "found unexpected dataset type {};"
                    " please notify the Verta development team".format(content_oneof)
                )

            # return list of component objects
            components = getattr(blob, content_oneof).components
            return list(map(component_cls._from_proto, components))

        return []


class RawDatasetVersion(DatasetVersion):
    def __init__(self, *args, **kwargs):
        super(RawDatasetVersion, self).__init__(*args, **kwargs)
        self.dataset_version_info = self.dataset_version.raw_dataset_version_info

    @staticmethod
    def make_create_message(dataset_id, dataset_type,
                            dataset_version_info,
                            parent_id=None,
                            desc=None, tags=None, attrs=None,
                            version=None):
        if tags is not None:
            tags = _utils.as_list_of_str(tags)
        Message = _DatasetVersionService.CreateDatasetVersion
        version_msg = _DatasetVersionService.RawDatasetVersionInfo
        converted_dataset_version_info = version_msg(
            size=dataset_version_info.size, features=dataset_version_info.features,
            num_records=dataset_version_info.num_records,
            object_path=dataset_version_info.object_path, checksum=dataset_version_info.checksum
        )
        msg = Message(dataset_id=dataset_id, parent_id=parent_id,
                      description=desc, tags=tags, dataset_type=dataset_type,
                      attributes=attrs, version=version,
                      raw_dataset_version_info=converted_dataset_version_info)
        return msg


class PathDatasetVersion(DatasetVersion):
    def __init__(self, *args, **kwargs):
        super(PathDatasetVersion, self).__init__(*args, **kwargs)
        self.dataset_version_info = self.dataset_version.path_dataset_version_info

    @staticmethod
    def make_create_message(dataset_id, dataset_type,
                            dataset_version_info,
                            parent_id=None,
                            desc=None, tags=None, attrs=None,
                            version=None):
        if tags is not None:
            tags = _utils.as_list_of_str(tags)
        Message = _DatasetVersionService.CreateDatasetVersion
        # turn dataset_version_info into proto format
        version_msg = _DatasetVersionService.PathDatasetVersionInfo
        converted_dataset_version_info = version_msg(
            location_type=dataset_version_info.location_type,
            size=dataset_version_info.size,
            dataset_part_infos=dataset_version_info.dataset_part_infos,
            base_path=dataset_version_info.base_path
        )
        msg = Message(dataset_id=dataset_id, parent_id=parent_id,
                      description=desc, tags=tags, dataset_type=dataset_type,
                      attributes=attrs, version=version,
                      path_dataset_version_info=converted_dataset_version_info)
        return msg


class QueryDatasetVersion(DatasetVersion):
    def __init__(self, *args, **kwargs):
        super(QueryDatasetVersion, self).__init__(*args, **kwargs)
        self.dataset_version_info = self.dataset_version.query_dataset_version_info

    @staticmethod
    def make_create_message(dataset_id, dataset_type,
                            dataset_version_info,
                            parent_id=None,
                            desc=None, tags=None, attrs=None,
                            version=None):
        if tags is not None:
            tags = _utils.as_list_of_str(tags)
        Message = _DatasetVersionService.CreateDatasetVersion
        version_msg = _DatasetVersionService.QueryDatasetVersionInfo
        converted_dataset_version_info = version_msg(
            query=dataset_version_info.query,
            query_template=dataset_version_info.query_template, query_parameters=dataset_version_info.query_parameters,
            data_source_uri=dataset_version_info.data_source_uri,
            execution_timestamp=dataset_version_info.execution_timestamp, num_records=dataset_version_info.num_records
        )
        msg = Message(dataset_id=dataset_id, parent_id=parent_id,
                      description=desc, tags=tags, dataset_type=dataset_type,
                      attributes=attrs, version=version,
                      # different dataset versions
                      query_dataset_version_info=converted_dataset_version_info)
        return msg


class PathDatasetVersionInfo(object):
    def __init__(self):
        self.dataset_part_infos = []

    def compute_dataset_size(self):
        self.size = 0
        for dataset_part_info in self.dataset_part_infos:
            self.size += dataset_part_info.size

    def get_dataset_part_infos(self):
        raise NotImplementedError('Implemented only in subclasses')


class FilesystemDatasetVersionInfo(PathDatasetVersionInfo):
    def __init__(self, path):
        self.base_path = os.path.abspath(os.path.expanduser(path))
        super(FilesystemDatasetVersionInfo, self).__init__()
        self.location_type = _DatasetVersionService.PathLocationTypeEnum.LOCAL_FILE_SYSTEM
        self.dataset_part_infos = self.get_dataset_part_infos()
        self.compute_dataset_size()

    def get_dataset_part_infos(self):
        dataset_part_infos = []
        # find all files there and create dataset_part_infos
        if os.path.isdir(self.base_path):
            dir_infos = os.walk(self.base_path)
            for root, _, filenames in dir_infos:
                for filename in filenames:
                    dataset_part_infos.append(self.get_file_info(root + "/" + filename))
        else:
            dataset_part_infos.append(self.get_file_info(self.base_path))
        # raise NotImplementedError('Only local files or S3 supported')
        return dataset_part_infos

    def get_file_info(self, path):
        dataset_part_info = _DatasetVersionService.DatasetPartInfo()
        dataset_part_info.path = path
        dataset_part_info.size = os.path.getsize(path)
        dataset_part_info.checksum = self.compute_file_hash(path)
        dataset_part_info.last_modified_at_source = _utils.timestamp_to_ms(int(os.path.getmtime(path)))
        return dataset_part_info

    def compute_file_hash(self, path):
        BLOCKSIZE = 65536
        hasher = hashlib.md5()
        with open(path, 'rb') as afile:
            buf = afile.read(BLOCKSIZE)
            while len(buf) > 0:
                hasher.update(buf)
                buf = afile.read(BLOCKSIZE)
        return hasher.hexdigest()


class S3DatasetVersionInfo(PathDatasetVersionInfo):
    def __init__(self, bucket_name, key=None, key_prefix=None, url_stub=None):
        super(S3DatasetVersionInfo, self).__init__()
        self.location_type = _DatasetVersionService.PathLocationTypeEnum.S3_FILE_SYSTEM
        self.bucket_name = bucket_name
        self.key = key
        self.key_prefix = key_prefix
        self.url_stub = url_stub
        self.base_path = ("" if url_stub is None else url_stub) + bucket_name \
            + (("/" + key) if key is not None else "")
        self.dataset_part_infos = self.get_dataset_part_infos()
        self.compute_dataset_size()

    def get_dataset_part_infos(self):
        boto3 = importer.maybe_dependency("boto3")
        if boto3 is None:  # Boto 3 not installed
            six.raise_from(ImportError("Boto 3 is not installed; try `pip install boto3`"), None)

        conn = boto3.client('s3')
        dataset_part_infos = []
        if self.key is not None:
            # look up object by key
            obj = conn.head_object(Bucket=self.bucket_name, Key=self.key)
            self._append_s3_object_info(dataset_part_infos, obj, self.key)
        elif self.key_prefix is not None:
            # look up objects by key prefix
            for obj in conn.list_objects(Bucket=self.bucket_name, Prefix=self.key_prefix)['Contents']:
                self._append_s3_object_info(dataset_part_infos, obj)
        else:
            # look up all objects in bucket
            for obj in conn.list_objects(Bucket=self.bucket_name)['Contents']:
                self._append_s3_object_info(dataset_part_infos, obj)
        return dataset_part_infos

    def _append_s3_object_info(self, dataset_part_infos, object_info, key=None):
        if self._is_folder(key or object_info['Key']):  # folder, not object
            return
        dataset_part_infos.append(self.get_s3_object_info(object_info, key))

    @staticmethod
    def _is_folder(key):
        return key.endswith('/')

    @staticmethod
    def get_s3_object_info(object_info, key=None):
        # S3 also provides version info that could be used:
        #     https://boto3.amazonaws.com/api/v1/modeldb/documentation/api/latest/reference/services/s3.html
        dataset_part_info = _DatasetVersionService.DatasetPartInfo()
        dataset_part_info.path = object_info['Key'] if key is None else key
        dataset_part_info.size = object_info['Size'] if key is None else object_info['ContentLength']
        dataset_part_info.checksum = object_info['ETag']
        dataset_part_info.last_modified_at_source = _utils.timestamp_to_ms(_utils.ensure_timestamp(object_info['LastModified']))
        return dataset_part_info


class QueryDatasetVersionInfo(object):
    def __init__(self, job_id=None,
                 query="", execution_timestamp="", data_source_uri="",
                 query_template="", query_parameters=[], num_records=0):
        if not query:
            raise ValueError("query not found")
        self.query = query
        self.execution_timestamp = execution_timestamp
        self.data_source_uri = data_source_uri
        self.query_template = query_template
        self.query_parameters = query_parameters
        self.num_records = num_records


class AtlasHiveDatasetVersionInfo(QueryDatasetVersionInfo):
    def __init__(self, guid="", atlas_url="",
                 atlas_user_name="", atlas_password="",
                 atlas_entity_endpoint="/api/atlas/v2/entity/bulk",
                 parent_id=None, desc=None, tags=None, attrs=None):
        if guid and atlas_url:
            self.guid = guid
            atlas_entity_details = self.get_entity_details(guid, atlas_url,
                                                           atlas_user_name, atlas_password, atlas_entity_endpoint)
            if len(atlas_entity_details['entities']) != 1:
                raise ValueError("Error fetching details of entity from Atlas")
            table_obj = atlas_entity_details['entities'][0]
            if table_obj['typeName'] != 'hive_table':
                raise NotImplementedError("Atlas dataset currently supported only for Hive tables")
            #TODO: what is the execution timestamp? Should the user log this later
            self.execution_timestamp = _utils.now()
            self.data_source_uri = atlas_url + "/index.html#!/detailPage/" + guid
            self.query = self.generate_query(table_obj)
            #TODO: extract the query template
            self.query_template = self.query
            self.query_parameters = []
            self.num_records = int(table_obj['attributes']['parameters']['numRows'])
            self.attributes = self.get_attributes(table_obj)
            self.tags = self.get_tags(table_obj)
        else:
            super(AtlasHiveDatasetVersionInfo, self).__init__()

    @staticmethod
    def get_tags(table_obj):
        verta_tags = []
        if 'classifications' in table_obj:
            atlas_classifications = table_obj['classifications']
            for atlas_classification in atlas_classifications:
                verta_tags.append(atlas_classification['typeName'])
        return verta_tags

    @staticmethod
    def get_entity_details(guid, atlas_url, atlas_user_name, atlas_password, atlas_entity_endpoint):
        response = requests.get(atlas_url + atlas_entity_endpoint,
                                auth=(atlas_user_name, atlas_password),
                                params={'guid': guid})
        return _utils.body_to_json(response)

    @staticmethod
    def generate_query(table_obj):
        table_name = table_obj['attributes']['name'] # store as attribute
        database_name = table_obj['relationshipAttributes']['db']['displayText'] #store as atrribute
        query = "select * from {}.{}".format(database_name, table_name)
        return query

    @staticmethod
    def get_attributes(table_obj):
        attribute_keyvals = []
        attributes = {}
        attributes['type'] = table_obj['typeName']
        attributes['table_name'] = table_obj['attributes']['name'] # store as attribute
        attributes['database_name'] = table_obj['relationshipAttributes']['db']['displayText'] #store as atrribute
        attributes['col_names'] = AtlasHiveDatasetVersionInfo.get_columns(table_obj)
        attributes['created_time'] = table_obj['createTime']
        attributes['update_time'] = table_obj['updateTime']
        attributes['load_queries'] = AtlasHiveDatasetVersionInfo.get_inbound_queries(table_obj)
        # for key, value in six.viewitems(attributes):
            # attribute_keyvals.append(_CommonCommonService.KeyValue(key=key,
            #                                                  value=_utils.python_to_val_proto(value, allow_collection=True)))
        # return attribute_keyvals
        return attributes

    @staticmethod
    def get_columns(table_obj):
        column_objs = table_obj['relationshipAttributes']['columns']
        col_names = []
        for column_obj in column_objs:
            col_names.append(column_obj['displayText'])
        return col_names

    @staticmethod
    def get_inbound_queries(table_obj):
        verta_input_processes = []
        atlas_input_processes = table_obj['relationshipAttributes']['outputFromProcesses']
        for atlas_input_process in atlas_input_processes:
            verta_input_processes.append(atlas_input_process['displayText'])
        return verta_input_processes

class BigQueryDatasetVersionInfo(QueryDatasetVersionInfo):
    def __init__(self, job_id=None,
                 query="", location="", execution_timestamp="", data_source_uri="",
                 query_template="", query_parameters=[], num_records=0):
        """https://googleapis.github.io/google-cloud-python/latest/bigquery/generated/google.cloud.bigquery.job.QueryJob.html#google.cloud.bigquery.job.QueryJob.query_plan"""
        if job_id is not None and location:
            self.job_id = job_id
            job = self.get_bq_job(job_id, location)
            self.execution_timestamp = _utils.timestamp_to_ms(_utils.ensure_timestamp(job.started))
            self.data_source_uri = job.self_link
            self.query = job.query
            #TODO: extract the query template
            self.query_template = job.query
            self.query_parameters = []
            self.num_records = job.to_dataframe().shape[0]
        else:
            super(BigQueryDatasetVersionInfo, self).__init__()

    @staticmethod
    def get_bq_job(job_id, location):
        bigquery = importer.maybe_dependency("google.cloud.bigquery")
        if bigquery is None:  # BigQuery not installed
            six.raise_from(ImportError("BigQuery is not installed;"
                                       " try `pip install google-cloud-bigquery`"),
                           None)

        client = bigquery.Client()
        return client.get_job(job_id, location=location)

class RDBMSDatasetVersionInfo(QueryDatasetVersionInfo):
    def __init__(self, query="", db_connection_str="", execution_timestamp=None,
                 query_template="", query_parameters=[], num_records=0):
        if execution_timestamp is None:
            self.execution_timestamp = _utils.timestamp_to_ms(time.time())
        self.data_source_uri = db_connection_str
        self.query = query
        self.query_template = query_template
        self.query_parameters = query_parameters
        self.num_records = num_records
