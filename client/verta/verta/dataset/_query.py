# -*- coding: utf-8 -*-

from . import _dataset

from .._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService
from .._protos.public.modeldb.versioning import Dataset_pb2 as _DatasetService


class QueryDataset(_dataset._Dataset):
    def __init__(self, query, data_source_uri=None, execution_timestamp=None, num_records=None):
        # paths probably do not apply here
        # also enable_mdb_versioning should be False (for now)
        super(QueryDataset, self).__init__(None, False)

        self.query = query
        self.data_source_uri = data_source_uri
        self.execution_timestamp = execution_timestamp
        self.num_records = num_records

    @classmethod
    def _from_proto(cls, blob_msg):
        query_msg = blob_msg.dataset.query
        component_msg = query_msg.components[0]

        return cls(
            component_msg.query,
            component_msg.data_source_uri,
            component_msg.execution_timestamp,
            component_msg.num_records
        )

    def _as_proto(self):
        blob_msg = _VersioningService.Blob()
        component_msg = _DatasetService.QueryDatasetComponentBlob(
            query=self.query,
            data_source_uri=self.data_source_uri,
            execution_timestamp=self.execution_timestamp,
            num_records=self.num_records
        )
        blob_msg.dataset.query.components.append(component_msg)

        return blob_msg

    def _prepare_components_to_upload(self):
        """
        This method does nothing because this query dataset does not support uploading yet.

        """
        return

    def _clean_up_uploaded_components(self):
        """
        This method does nothing because this query dataset does not support uploading yet.

        """
        return

    def add(self, paths):
        return
