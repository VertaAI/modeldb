# -*- coding: utf-8 -*-
"""Queries for summaries and summary samples."""

from __future__ import print_function

from verta import data_types
from verta._internal_utils import arg_handler, pagination_utils, time_utils
from verta._internal_utils._utils import as_list_of_str
from verta._protos.public.monitoring.Summary_pb2 import (
    AggregationQuerySummary,
    FilterQuerySummarySample,
    FindSummaryRequest,
    FindSummarySampleRequest,
    LabelFilterQuerySummarySample,
)

from .aggregation import Aggregation


def _labels_proto(labels):
    return {
        key: LabelFilterQuerySummarySample(label_value=as_list_of_str(values))
        for key, values in labels.items()
    }


class SummaryQuery(object):
    """
    A query for summaries.

    Parameters
    ----------
    ids : list of int, optional
        Only fetch these summaries.
    names : list of str, optional
        Only fetch these summaries with one of these names.
    data_type_classes : list of :mod:`VertaDataType <verta.data_types>`, optional
        Only fetch summaries with one of these data types.
    monitored_entities : list of :class:`~verta.monitoring.monitored_entity.MonitoredEntity`, optional
        Only fetch summaries belonging to one of these monitored entities.
    page_number : int, default 1
        Pagination page number for the backend query request. Used in
        conjunction with `page_limit`.
    page_limit : int, optional
        Number of samples to fetch from the backend in a single query. If not
        provided, all accessible samples will be fetched.

    Examples
    --------
    .. code-block:: python

        from datetime import datetime, timezone
        from verta.monitoring.summary import SummaryQuery, SummarySampleQuery
        from verta.data_types import FloatHistogram, DiscreteHistogram

        summary_query = SummaryQuery(
            names=["Income Distributions"]),
            data_types=[FloatHistogram, DiscreteHistogram],
        )

        client = Client()
        for summary in client.monitoring.summaries.find(sample_query):
            print(summary)
    """

    def __init__(
        self,
        ids=None,
        names=None,
        data_type_classes=None,
        monitored_entities=None,
        page_number=1,
        page_limit=None,
    ):
        self._ids = arg_handler.extract_ids(ids) if ids else None
        self._names = names

        self._initialize_data_types(data_type_classes)
        self._monitored_entity_ids = (
            arg_handler.extract_ids(monitored_entities) if monitored_entities else None
        )
        self._page_number = page_number
        self._page_limit = page_limit

    def __eq__(self, other):
        if not isinstance(other, type(self)):
            return NotImplemented

        return self._to_proto_request() == other._to_proto_request()

    @property
    def monitored_entity_ids(self):
        return self._monitored_entity_ids

    @property
    def data_type_classes(self):
        return self._data_type_classes

    @property
    def type_names(self):
        return self._type_names

    def _initialize_data_types(self, classes_or_strings):
        self._type_names = None
        self._data_type_classes = None
        if classes_or_strings:
            try:
                self._type_names = [cls._type_string() for cls in classes_or_strings]
                self._data_type_classes = classes_or_strings
            except AttributeError:
                self._type_names = [type_string for type_string in classes_or_strings]
                self._data_type_classes = data_types._VertaDataType._from_type_strings(
                    classes_or_strings
                )

    @classmethod
    def _from_proto_request(cls, msg):
        types = map(data_types._VertaDataType._from_type_string, msg.type_names)
        types = [dt for dt in types if dt is not None]
        return cls(
            ids=msg.ids,
            names=msg.names,
            data_type_classes=types,
            monitored_entities=msg.monitored_entity_ids,
            page_number=msg.page_number,
            page_limit=pagination_utils.page_limit_from_proto(msg.page_limit),
        )

    def _to_proto_request(self):
        return FindSummaryRequest(
            ids=self._ids,
            names=self._names,
            type_names=self._type_names,
            monitored_entity_ids=self._monitored_entity_ids,
            page_number=self._page_number,
            page_limit=pagination_utils.page_limit_to_proto(self._page_limit),
        )

    def __repr__(self):
        return "SummaryQuery({}, {}, {}, {})".format(
            self._ids, self._names, self._type_names, self._monitored_entity_ids
        )


class SummarySampleQuery(object):
    """
    A query for summary samples.

    Parameters
    ----------
    summary_query : :class:`SummaryQuery`, optional
        Only fetch samples whose summaries match this query.
    ids : list of int, optional
        Only fetch these samples.
    labels : dict of str to list of str, optional
        Only fetch samples that have at least one of these labels. A mapping
        between label keys and lists of corresponding label values.
    time_window_start : datetime.datetime or int, optional
        Only fetch samples whose time windows start at or after this time.
        Either a timezone aware datetime object or unix epoch milliseconds.
    time_window_end : datetime.datetime or int, optional
        Only fetch samples whose time windows end at or before this time.
        Either a timezone aware datetime object or unix epoch milliseconds.
    created_after : datetime.datetime or int, optional
        Only fetch samples created at or after this time. Either a timezone
        aware datetime object or unix epoch milliseconds.
    aggregation : :class:`~verta.monitoring.summaries.aggregation.Aggregation`, optional
        Parameters for aggregation of summary samples.
    page_number : int, default 1
        Pagination page number for the backend query request. Used in
        conjunction with `page_limit`.
    page_limit : int, optional
        Number of samples to fetch from the backend in a single query. If not
        provided, all accessible samples will be fetched.

    Examples
    --------
    .. code-block:: python

        from datetime import datetime, timedelta, timezone
        from verta.monitoring.summaries.aggregation import Aggregation
        from verta.monitoring.summaries.queries import SummaryQuery, SummarySampleQuery

        samples = Client().monitoring.summary_samples

        sample_query = SummarySampleQuery(
            summary_query=SummaryQuery(names=["Income Distributions"]),
            labels={"datasource": ["census2010", "census2020"]},
            created_after=datetime(year=2021, month=2, day=22, tzinfo=timezone.utc),
            aggregation=Aggregation(timedelta(days=7), "sum")
        )

        for sample in samples.find(sample_query):
            print(sample.content)

    """

    def __init__(
        self,
        summary_query=None,
        ids=None,
        labels=None,
        time_window_start=None,
        time_window_end=None,
        aggregation=None,
        created_after=None,
        page_number=1,
        page_limit=None,
    ):
        if summary_query is None:
            summary_query = SummaryQuery()

        self._summary_query = summary_query
        self._sample_ids = arg_handler.extract_ids(ids) if ids else None
        self._labels = arg_handler.maybe(_labels_proto, labels)
        self._time_window_start = time_window_start
        self._time_window_end = time_window_end
        self.aggregation = aggregation
        self._created_after = created_after
        self._page_number = page_number
        self._page_limit = page_limit

    def __eq__(self, other):
        if not isinstance(other, type(self)):
            return NotImplemented

        return self._to_proto_request() == other._to_proto_request()

    @property
    def summary_query(self):
        return self._summary_query

    @summary_query.setter
    def summary_query(self, query):
        self._summary_query = query

    @property
    def _find_summaries(self):
        return self.summary_query._to_proto_request()

    @_find_summaries.setter
    def _find_summaries(self, proto_summary_query):
        summary_query = SummaryQuery._from_proto_request(proto_summary_query)
        self._summary_query = summary_query

    @property
    def aggregation(self):
        return self._aggregation

    @aggregation.setter
    def aggregation(self, value):
        if value is None:
            self._aggregation = None
        elif isinstance(value, AggregationQuerySummary):
            self._aggregation = Aggregation._from_proto(value)
        elif isinstance(value, Aggregation):
            self._aggregation = value
        else:
            raise ValueError(
                "value must be Aggregation object or proto, not {}".format(type(value))
            )

    @classmethod
    def _from_proto_request(cls, msg):
        # set attrs after creation to bypass conversion logic in __init__()
        obj = cls()
        obj._find_summaries = msg.filter.find_summaries
        obj._sample_ids = msg.filter.sample_ids
        obj._labels = msg.filter.labels
        obj._time_window_start = time_utils.datetime_from_millis(
            msg.filter.time_window_start_at_millis
        )
        obj._time_window_end = time_utils.datetime_from_millis(
            msg.filter.time_window_end_at_millis
        )
        obj._created_after = time_utils.datetime_from_millis(
            msg.filter.created_at_after_millis
        )
        obj.aggregation = msg.aggregation
        obj._page_number = msg.page_number
        obj._page_limit = pagination_utils.page_limit_from_proto(msg.page_limit)

        return obj

    def _to_proto_request(self):
        aggregation_proto = arg_handler.maybe(lambda agg: agg._to_proto(), self.aggregation)
        return FindSummarySampleRequest(
            filter=FilterQuerySummarySample(
                find_summaries=self._find_summaries,
                sample_ids=self._sample_ids,
                labels=self._labels,
                time_window_start_at_millis=time_utils.epoch_millis(
                    self._time_window_start
                ),
                time_window_end_at_millis=time_utils.epoch_millis(
                    self._time_window_end
                ),
                created_at_after_millis=time_utils.epoch_millis(self._created_after),
            ),
            aggregation=aggregation_proto,
            page_number=self._page_number,
            page_limit=pagination_utils.page_limit_to_proto(self._page_limit),
        )

    def __repr__(self):
        return "SummarySampleQuery({})".format(self._to_proto_request())
