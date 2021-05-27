# -*- coding: utf-8 -*-
"""An entity to create and contain samples, persisted to Verta."""

from __future__ import print_function

import json

from verta._internal_utils import time_utils
from verta._protos.public.monitoring import Summary_pb2 as SummaryService
from verta._protos.public.monitoring.Summary_pb2 import (
    CreateSummarySample,
    DeleteSummarySampleRequest,
    Empty as EmptyProto,
    FindSummarySampleRequest,
    SummarySample as SummarySampleProto,
)
from verta.tracking.entities import _entity
from verta import data_types
from verta.monitoring.alert.entities import Alerts
from .queries import SummarySampleQuery
from .summary_sample import SummarySample


class Summary(_entity._ModelDBEntity):
    """A summary object to validate and aggregate summary samples.

    Users should obtain summaries through one of the query or create methods of
    the ``summaries`` attribute on the monitoring
    sub-:class:`~verta.monitoring.client.Client` instead of
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
        self._monitored_entity_id = msg.monitored_entity_id  # TODO: hide me
        self.name = msg.name
        self.type = msg.type_name  # TODO: hide me

    def __repr__(self):
        return "Summary name:{}, type:{}, monitored_entity_id:{}".format(
            self.name, self.type, self.monitored_entity_id
        )

    @property
    def alerts(self):
        return Alerts(self._conn, self._conf, self.monitored_entity_id, summary=self)

    @property
    def monitored_entity_id(self):
        return self._monitored_entity_id

    def log_sample(
        self, data, labels, time_window_start, time_window_end, created_at=None
    ):
        """Log a summary sample for this summary.

        Parameters
        ----------
        data
            A :mod:`VertaDataType <verta.data_types>` consistent with the type of this summary.
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
        :class:`~verta.monitoring.summaries.summary_sample.SummarySample`
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
        query : :class:`~verta.monitoring.summaries.queries.SummarySampleQuery`, optional
            A query object which filters the set of summary samples.

        Returns
        -------
        list of :class:`~verta.monitoring.summaries.summary_sample.SummarySample`
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

    def delete_samples(self, summary_samples):
        """Delete summary samples from this summary.

        Parameters
        ----------
        summary_samples : list of :class:`~verta.monitoring.summaries.summary_sample.SummarySample`
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

    def delete(self):
        """
        Delete this summary.

        Returns
        -------
        bool
            ``True`` if the delete was successful.

        Raises
        ------
        :class:`requests.HTTPError`
            If the delete failed.

        """
        msg = SummaryService.DeleteSummaryRequest(ids=[self.id])
        endpoint = "/api/v1/summaries/deleteSummary"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True
