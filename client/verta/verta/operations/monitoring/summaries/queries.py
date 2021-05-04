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
    data_type_classes : list of :class:`~verta.data_types._VertaDataType`, optional
        Only fetch summaries with one of these data types.
    monitored_entities : list of :class:`~verta.operations.monitoring.monitored_entity.MonitoredEntity`, optional
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
        from verta.operations.monitoring.summary import SummaryQuery, SummarySampleQuery
        from verta.data_types import FloatHistogram, DiscreteHistogram

        summary_query = SummaryQuery(
            names=["Income Distributions"]),
            data_types=[FloatHistogram, DiscreteHistogram],
        )

        client = Client()
        for summary in client.operations.summaries.find(sample_query):
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
        self._ids = extract_ids(ids) if ids else None
        self._names = names

        if data_type_classes:
            try:
                self._type_names = [cls._type_string() for cls in data_type_classes]
            except AttributeError:
                self._type_names = [type_string for type_string in data_type_classes]
        else:
            self._type_names = None

        self._monitored_entity_ids = (
            extract_ids(monitored_entities) if monitored_entities else None
        )
        self._page_number = page_number
        self._page_limit = page_limit

    @property
    def monitored_entity_ids(self):
        return self._monitored_entity_ids

    @property
    def data_type_classes(self):
        return self._data_type_classes

    @data_type_classes.setter
    def data_type_classes(self, type_classes):
        if type_classes:
            type_names = [cls._type_string() for cls in type_classes]
            self._type_names = type_names
            self._data_type_classes = type_classes
        else:
            self._type_names = None
            self._data_type_classes = None

    @property
    def type_names(self):
        return self._type_names

    @type_names.setter
    def type_names(self, names):
        if names:
            type_classes = data_types._VertaDataType._from_type_strings(names)
            self._data_type_classes = type_classes
            self._type_names = names
        else:
            self._data_type_classes = None
            self._type_names = None

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

    def __add__(self, other):
        assert isinstance(other, self.__class__)
        ids = self._ids or other._ids
        names = self._names or other._names
        data_type_classes = self._type_names or other._type_names
        monitored_entities = self._monitored_entity_ids or other._monitored_entity_ids
        page_number = self._page_number or other._page_number
        page_limit = self._page_limit or other._page_limit
        return SummaryQuery(
            ids=ids,
            names=names,
            data_type_classes=data_type_classes,
            monitored_entities=monitored_entities,
            page_number=page_number,
            page_limit=page_limit,
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
        from verta.operations.monitoring.summary import SummaryQuery, SummarySampleQuery

        samples = Client().operations.summary_samples
        sample_query = SummarySampleQuery(
            summary_query=SummaryQuery(names=["Income Distributions"]),
            labels={"datasource": ["census2010", "census2020"]},
            created_after=datetime(year=2021, month=2, day=22, tzinfo=timezone.utc),
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
        created_after=None,
        page_number=1,
        page_limit=None,
    ):
        if summary_query is None:
            summary_query = SummaryQuery()

        self._summary_query = summary_query
        self._sample_ids = extract_ids(ids) if ids else None
        self._labels = maybe(_labels_proto, labels)
        self._time_window_start = time_window_start
        self._time_window_end = time_window_end
        self._created_after = created_after
        self._page_number = page_number
        self._page_limit = page_limit

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
        obj._page_number = msg.page_number
        obj._page_limit = pagination_utils.page_limit_from_proto(msg.page_limit)

        return obj

    def _to_proto_request(self):
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
            page_number=self._page_number,
            page_limit=pagination_utils.page_limit_to_proto(self._page_limit),
        )

    def __repr__(self):
        return "SummarySampleQuery({})".format(self._to_proto_request())

    def _set_created_after(self, created_after):
        """To avoid having the alerter assign directly to a private attr."""
        self._created_after = created_after
