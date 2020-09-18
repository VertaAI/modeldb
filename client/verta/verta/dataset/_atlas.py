# -*- coding: utf-8 -*-

from . import QueryDataset

from .._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService
from .._protos.public.modeldb.versioning import Dataset_pb2 as _DatasetService

from .._internal_utils import _utils

import requests


class AtlasDataset(QueryDataset):
    def __init__(self, guid, atlas_url="", atlas_user_name="", atlas_password="", atlas_entity_endpoint="/api/atlas/v2/entity/bulk"):
        atlas_entity_details = AtlasDataset.get_entity_details(guid, atlas_url, atlas_user_name, atlas_password, atlas_entity_endpoint)

        if len(atlas_entity_details['entities']) != 1:
            raise ValueError("Error fetching details of entity from Atlas")

        table_obj = atlas_entity_details['entities'][0]
        if table_obj['typeName'] != 'hive_table':
            raise NotImplementedError("Atlas dataset currently supported only for Hive tables")

        execution_timestamp = _utils.now()
        data_source_uri = "{}/index.html#!/detailPage/{}".format(atlas_url, guid)
        query = AtlasDataset.generate_query(table_obj)
        num_records = int(table_obj['attributes']['parameters']['numRows'])

        super(AtlasDataset, self).__init__(query, data_source_uri, execution_timestamp, num_records)

        self.attributes = AtlasDataset.get_attributes(table_obj)
        self.tags = AtlasDataset.get_tags(table_obj)

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
        attributes['col_names'] = AtlasDataset.get_columns(table_obj)
        attributes['created_time'] = table_obj['createTime']
        attributes['update_time'] = table_obj['updateTime']
        attributes['load_queries'] = AtlasDataset.get_inbound_queries(table_obj)
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
