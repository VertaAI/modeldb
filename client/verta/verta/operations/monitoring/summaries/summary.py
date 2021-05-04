# -*- coding: utf-8 -*-

from __future__ import print_function
import warnings

import json
from datetime import datetime

from verta._internal_utils._utils import as_list_of_str
from verta._internal_utils import pagination_utils, time_utils
from ..utils import extract_ids, extract_id, maybe
from verta._protos.public.monitoring import Summary_pb2 as SummaryService
from verta._protos.public.monitoring.Summary_pb2 import (
    CreateSummaryRequest,
    CreateSummarySample,
    DeleteSummaryRequest,
    DeleteSummarySampleRequest,
    Empty as EmptyProto,
    FilterQuerySummarySample,
    FindSummaryRequest,
    FindSummarySampleRequest,
    LabelFilterQuerySummarySample,
    Summary as SummaryProto,
    SummarySample as SummarySampleProto,
)
from verta._tracking import entity
from verta import data_types
from verta.operations.monitoring.alert._entities import Alerts
from .queries import SummaryQuery, SummarySampleQuery
from .summary_sample import SummarySample


class Summary(entity._ModelDBEntity):
    """A summary object to validate and aggregate summary samples.

    Users should obtain summaries through one of the query or create methods of
    the ``summaries`` attribute on the operations
    sub-:class:`~verta.opertaions.monitoring.client.Client` instead of
    initializing Summary objects.

    Parameters
    ----------
    conn
        A connection object to the backend service.
    conf
        A configuration object used by conn methods.
    msg
        A protobuf message ai.verta.monitoring.Summary

    Attributes
    ----------
    name: str
        The name of this summary.
    """

    def __init__(self, conn, conf, msg):
        super(Summary, self).__init__(conn, conf, SummaryService, "summary", msg)
        self._conn = conn
        self._conf = conf
        self.monitored_entity_id = msg.monitored_entity_id  # TODO: hide me
        self.name = msg.name
        self.type = msg.type_name  # TODO: hide me

        alerts_query = SummaryQuery(ids=[self.id])
        self._alerts = Alerts(
            conn, conf, self.monitored_entity_id, base_summary_query=alerts_query
        )

    def __repr__(self):
        return "Summary name:{}, type:{}, monitored_entity_id:{}".format(
            self.name, self.type, self.monitored_entity_id
        )

    @property
    def alerts(self):
        return self._alerts

    def log_sample(
        self, data, labels, time_window_start, time_window_end, created_at=None
    ):
        """Log a summary sample for this summary.

        Parameters
        ----------
        data
            A :class:`~verta.data_types._VertaDataType` consistent with the type of this summary.
        labels : dict of str to str, optional
            A mapping between label keys and values.
        time_window_start : datetime.datetime or int
            Either a timezone aware datetime object or unix epoch milliseconds.
        time_window_end : datetime.datetime or int
            Either a timezone aware datetime object or unix epoch milliseconds.
        created_after : datetime.datetime or int, optional
            Either a timezone aware datetime object or unix epoch milliseconds.
            Defaults to now, but offered as a parameter to permit backfilling of
            summary samples.

        Returns
        -------
        :class:`SummarySample`
            A persisted summary sample.
        """
        if not isinstance(data, data_types._VertaDataType):
            raise TypeError(
                "expected a supported VertaDataType, found {}".format(type(data))
            )
        if data._type_string() != self.type:
            raise TypeError(
                "expected a {}, found {}".format(self.type, data._type_string())
            )

        if not created_at:
            created_at = time_utils.now()

        content = json.dumps(data._as_dict())

        created_at_millis = time_utils.epoch_millis(created_at)
        window_start_millis = time_utils.epoch_millis(time_window_start)
        window_end_millis = time_utils.epoch_millis(time_window_end)

        msg = CreateSummarySample(
            summary_id=self.id,
            summary_type_name=data._type_string(),
            content=content,
            labels=labels,
            created_at_millis=created_at_millis,
            time_window_start_at_millis=window_start_millis,
            time_window_end_at_millis=window_end_millis,
        )

        endpoint = "/api/v1/summaries/createSample"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        result_msg = self._conn.must_proto_response(response, SummarySampleProto)
        return SummarySample(self._conn, self._conf, result_msg)

    def find_samples(self, query=None):
        """Find summary samples belonging to this summary.

        Parameters
        ----------
        query : :class:`SummarySampleQuery`, optional
            A query object which filters the set of summary samples.

        Returns
        -------
        list of :class:`SummarySample`
            A list of summary samples belonging to this summary and matching the
            query.
        """
        if query is None:
            query = SummarySampleQuery()
        msg = query._to_proto_request()
        if self.id not in msg.filter.find_summaries.ids:
            msg.filter.find_summaries.ids.append(self.id)

        endpoint = "/api/v1/summaries/findSample"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        success = self._conn.must_proto_response(
            response, FindSummarySampleRequest.Response
        )
        samples = [
            SummarySample(self._conn, self._conf, record) for record in success.samples
        ]
        return samples

    def has_type(self, data_type_cls):  # TODO: hideme
        return self.type == data_type_cls._type_string()

    # @staticmethod
    # def _labels_proto(labels):
    #     return {
    #         key: LabelFilterQuerySummarySample(label_value=as_list_of_str(values))
    #         for key, values in labels.items()
    #     }

    def delete(self, summary_samples):
        """Delete summary samples from this summary.

        Parameters
        ----------
        summary_samples : list of :class:`SummarySample`
            The summary samples which should be deleted from this summary.

        Returns
        -------
        bool
            True if the delete was successful.
        """
        try:
            ids = [sample.id for sample in summary_samples]
        except:
            ids = summary_samples
        endpoint = "/api/v1/summaries/deleteSample"
        msg = DeleteSummarySampleRequest(ids=ids)
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_proto_response(response, EmptyProto)
        return True
