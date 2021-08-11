# -*- coding: utf-8 -*-
"""A collection of summaries."""

from __future__ import print_function
import warnings


from verta._protos.public.monitoring.Summary_pb2 import (
    CreateSummaryRequest,
    DeleteSummaryRequest,
    Empty as EmptyProto,
    FindSummaryRequest,
    Summary as SummaryProto,
)
from verta import data_types
from verta._internal_utils import arg_handler

from .summary import Summary
from .queries import SummaryQuery


class Summaries:
    """Collection object for creating and finding summaries.

    Parameters
    ----------
    conn
        A connection object to the backend service.
    conf
        A configuration object used by conn methods.

    Examples
    --------
    .. code-block:: python

        from datetime import datetime, timedelta, timezone

        from verta import Client
        from verta.monitoring.summaries.queries import SummaryQuery, SummarySampleQuery
        from verta import data_types

        client = Client()
        monitored = client.monitoring.get_or_create_monitored_entity()
        summary = client.monitoring.summaries.create(
            "predicted class", data_types.DiscreteHistogram, monitored
        )

        now = datetime.now(timezone.utc)
        yesterday = now - timedelta(days=1)

        predicted_classes = data_types.DiscreteHistogram(
            buckets=["spam", "important", "other"], data=[100, 20, 800]
        )
        labels = {"source": "training"}
        summary.log_sample(
            predicted_classes,
            labels=labels,
            time_window_start=yesterday,
            time_window_end=now
        )
        summary_samples = summary.find_samples(SummarySampleQuery(labels={"source": ["training"]}))
        for sample in summary_samples:
            print(sample)
    """

    def __init__(self, conn, conf):
        self._conn = conn
        self._conf = conf

    def create(self, name, data_type_cls, monitored_entity):  # TODO: hideme
        if not issubclass(data_type_cls, data_types._VertaDataType):
            raise TypeError(
                "expected a supported VertaDataType, found {}".format(
                    type(data_type_cls)
                )
            )
        msg = CreateSummaryRequest(
            monitored_entity_id=monitored_entity.id,
            name=name,
            type_name=data_type_cls._type_string(),
        )
        endpoint = "/api/v1/summaries/createSummary"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        proto = self._conn.must_proto_response(response, SummaryProto)
        return Summary(self._conn, self._conf, proto)

    def get_or_create(self, name, data_type_cls, monitored_entity):
        """Get or create a summary by name and data type.

        Parameters
        ----------
        name : str
            The name of this summary.
        data_type_cls: :mod:`VertaDataType <verta.data_types>`
            The class of data type which summary samples must conform to.
        monitored_entity: :class:`~verta.monitoring.monitored_entity.MonitoredEntity`
            A monitored entity object.

        Returns
        -------
        :class:`~verta.monitoring.summaries.summary.Summary`
            A retrieved or created summary.
        """
        if not issubclass(data_type_cls, data_types._VertaDataType):
            raise TypeError(
                "expected a supported VertaDataType, found {}".format(
                    type(data_type_cls)
                )
            )
        query = SummaryQuery(names=[name], monitored_entities=[monitored_entity])
        retrieved = self.find(query)
        # if retrieved and len(retrieved) > 1:
        #     warnings.warn(
        #         "found multiple summaries with name: {}, for monitored entity: {}".format(
        #             name, monitored_entity
        #         )
        #     )
        if retrieved:
            monitored_entity_id = arg_handler.extract_id(monitored_entity)
            cond = (
                lambda s: s.name == name
                and s.monitored_entity_id == monitored_entity_id
            )
            retrieved = list(filter(cond, retrieved))
        if retrieved:
            summary = retrieved[0]
        else:
            summary = self.create(name, data_type_cls, monitored_entity)
        if not summary.has_type(data_type_cls):
            warnings.warn(
                "retrieved summary has type {} although type {} was specified for create".format(
                    summary.type, data_type_cls._type_string()
                )
            )
        return summary

    def find(self, query=None):
        """Find summaries.

        Parameters
        ----------
        query : :class:`~verta.monitoring.summaries.queries.SummarySampleQuery`, optional
            A query object which filters the set of summary samples.

        Returns
        -------
        list of :class:`~verta.monitoring.summaries.summary_sample.SummarySample`
            A list of summary samples matching the query.
        """
        if query is None:
            query = SummaryQuery()
        elif not isinstance(query, SummaryQuery):
            raise TypeError(
                "`query` must be a SummaryQuery, not {}".format(type(query))
            )
        msg = query._to_proto_request()
        endpoint = "/api/v1/summaries/findSummary"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        maybe_summaries = self._conn.maybe_proto_response(
            response, FindSummaryRequest.Response
        )
        maybe_summaries = [
            Summary(self._conn, self._conf, msg) for msg in maybe_summaries.summaries
        ]
        return maybe_summaries

    def delete(self, summaries):
        """Delete the specified summaries.

        Parameters
        ----------
        summaries : list of :class:`~verta.monitoring.summaries.summary.Summary`
            The summaries which should be deleted.

        Returns
        -------
        bool
            True if the delete was successful.
        """
        summary_ids = [summary.id for summary in summaries]
        msg = DeleteSummaryRequest(ids=summary_ids)
        endpoint = "/api/v1/summaries/deleteSummary"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_proto_response(response, EmptyProto)
        return True
