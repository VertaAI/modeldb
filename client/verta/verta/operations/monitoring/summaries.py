# -*- coding: utf-8 -*-

from __future__ import print_function
import warnings

import json
from datetime import datetime

from verta._internal_utils._utils import as_list_of_str
from verta._internal_utils import pagination_utils, time_utils
from .utils import extract_ids, extract_id, maybe
from verta._protos.public.monitoring import Summary_pb2 as SummaryService
from verta._protos.public.monitoring.Summary_pb2 import (
    CreateSummaryRequest,
    CreateSummarySample,
    DeleteSummaryRequest,
    DeleteSummarySampleRequest,
    Empty as EmptyProto,
    FilterQuerySummarySample,
    FindSummaryRequest
    FindSummarySampleRequest,
    LabelFilterQuerySummarySample,
    Summary as SummaryProto,
    SummarySample as SummarySampleProto,
)
from verta._tracking import entity
from verta import data_types


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
            self._type_names = [cls._type_string() for cls in data_type_classes]
        else:
            self._type_names = None

        self._monitored_entity_ids = (
            extract_ids(monitored_entities) if monitored_entities else None
        )
        self._page_number = page_number
        self._page_limit = page_limit

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

        self._find_summaries = summary_query._to_proto_request()
        self._sample_ids = extract_ids(ids) if ids else None
        self._labels = maybe(Summary._labels_proto, labels)
        self._time_window_start = time_window_start
        self._time_window_end = time_window_end
        self._created_after = created_after
        self._page_number = page_number
        self._page_limit = page_limit

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

    def __repr__(self):
        return "Summary name:{}, type:{}, monitored_entity_id:{}".format(
            self.name, self.type, self.monitored_entity_id
        )

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

    @staticmethod
    def _labels_proto(labels):
        return {
            key: LabelFilterQuerySummarySample(label_value=as_list_of_str(values))
            for key, values in labels.items()
        }

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


class SummarySample(entity._ModelDBEntity):
    """A summary sample object capturing data for later comparison.

    Users should obtain summary samples through one of the query or create
    methods on a :class:`Summary` or the ``summary_samples`` attribute on the
    operations sub-:class:`~verta.opertaions.monitoring.client.Client` instead
    of initializing SummarySample objects directly.

    Parameters
    ----------
    conn
        A connection object to the backend service.
    conf
        A configuration object used by conn methods.
    msg
        A protobuf message ai.verta.monitoring.SummarySample

    Attributes
    ----------
    content
        A :class:`~verta.data_types._VertaDataType` consistent with the type of this summary.
    labels : dict of str to str, optional
        A mapping between label keys and values.
    time_window_start : datetime.datetime or int
        Either a timezone aware datetime object or unix epoch milliseconds.
    time_window_end : datetime.datetime or int
        Either a timezone aware datetime object or unix epoch milliseconds.
    created_after : datetime.datetime or int, optional
        Either a timezone aware datetime object or unix epoch milliseconds.

    Examples
    --------
    .. code-block:: python

        sample = summary.log_sample(
            predicted_classes,
            labels=labels,
            time_window_start=yesterday,
            time_window_end=now,
        )

    """

    def __init__(self, conn, conf, msg):
        super(SummarySample, self).__init__(conn, conf, SummaryService, "summary", msg)
        self.summary_id = msg.summary_id
        self.labels = msg.labels
        self.content = self._maybe_deserialize_datatype(msg.content)
        self.created_at = time_utils.datetime_from_millis(msg.created_at_millis)
        self.time_window_start_at = time_utils.datetime_from_millis(
            msg.time_window_start_at_millis
        )
        self.time_window_end_at = time_utils.datetime_from_millis(
            msg.time_window_end_at_millis
        )

    def __repr__(self):
        return "\n\t".join(
            (
                "SummarySample",
                "summary_id: {}".format(self.summary_id),
                "sample_id: {}".format(self.id),
                "content: {}".format(self.content),
                "labels: {}".format(self.labels),
                "window: [{}, {})".format(
                    self.time_window_start_at, self.time_window_end_at
                ),
            )
        )

    @staticmethod
    def _maybe_deserialize_datatype(content):
        try:
            return data_types._VertaDataType._from_dict(json.loads(content))
        except:
            return content


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
        from verta.operations.monitoring.summaries import SummaryQuery, SummarySampleQuery
        from verta import data_types

        client = Client()
        monitored = client.operations.get_or_create_monitored_entity()
        summary = client.operations.summaries.create(
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

    def create(self, name, data_type_cls, monitored_entity): # TODO: hideme
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
        data_type_cls: :class:`~verta.data_types._VertaDataType`
            The class of data type which summary samples must conform to.
        monitored_entity: :class:`~verta.operations.monitoring.monitored_entity.MonitoredEntity`
            A monitored entity object.

        Returns
        -------
        :class:`Summary`
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
            monitored_entity_id = extract_id(monitored_entity)
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
        query : :class:`SummarySampleQuery`, optional
            A query object which filters the set of summary samples.

        Returns
        -------
        list of :class:`SummarySample`
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
        summaries : list of :class:`Summary`
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


class SummarySamples:
    """Collection object for finding summary samples.

    Parameters
    ----------
    conn
        A connection object to the backend service.
    conf
        A configuration object used by conn methods.

    Examples
    --------
    .. code-block:: python

        from verta import Client

        client = Client()
        summary_samples = client.operations.summary_samples

    """

    def __init__(self, conn, conf):
        # TODO: potentially summary_id
        self._conn = conn
        self._conf = conf

    # TODO: potentially create()

    def find(self, query=None):
        """Find summary samples.

        Parameters
        ----------
        query : :class:`SummarySampleQuery`, optional
            A query object which filters the set of summary samples.

        Returns
        -------
        list of :class:`SummarySample`
            A list of summary samples matching the query.
        """
        if query is None:
            query = SummarySampleQuery()
        elif not isinstance(query, SummarySampleQuery):
            raise TypeError(
                "`query` must be a SummarySampleQuery, not {}".format(type(query))
            )
        msg = query._to_proto_request()
        endpoint = "/api/v1/summaries/findSample"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        maybe_samples = self._conn.must_proto_response(response, msg.Response)
        return [
            SummarySample(self._conn, self._conf, sample)
            for sample in maybe_samples.samples
        ]

    def delete(self, summary_samples):
        """Delete the specified summary samples.

        Parameters
        ----------
        summary_samples : list of :class:`SummarySample`
            The summary samples which should be deleted from this summary.

        Returns
        -------
        bool
            True if the delete was successful.
        """
        summary_sample_ids = extract_ids(summary_samples)
        msg = DeleteSummarySampleRequest(ids=summary_sample_ids)
        endpoint = "/api/v1/summaries/deleteSample"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True
