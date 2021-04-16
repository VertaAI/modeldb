# -*- coding: utf-8 -*-

from __future__ import print_function

import json
from datetime import datetime
from verta._internal_utils import pagination_utils, time_utils
from .utils import extract_ids, maybe
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


class SummaryQuery(object):
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
        return "SummaryQuery({}, {}, {}, {})".format(self._ids, self._names, self._type_names, self._monitored_entity_ids)


class SummarySampleQuery(object):
    def __init__(
        self,
        summary_query=None,
        ids=None,
        labels=None,
        time_window_start_at_millis=None,
        time_window_end_at_millis=None,
        created_at_after_millis=None,
        page_number=1,
        page_limit=None,
    ):
        if summary_query is None:
            summary_query = SummaryQuery()

        self._find_summaries = summary_query._to_proto_request()
        self._sample_ids = extract_ids(ids) if ids else None
        self._labels = maybe(Summary._labels_proto, labels)
        self._time_window_start_at_millis = time_window_start_at_millis
        self._time_window_end_at_millis = time_window_end_at_millis
        self._created_at_after_millis = created_at_after_millis
        self._page_number = page_number
        self._page_limit = page_limit

    @classmethod
    def _from_proto_request(cls, msg):
        # set attrs after creation to bypass conversion logic in __init__()
        obj = cls()
        obj._find_summaries = msg.filter.find_summaries
        obj._sample_ids = msg.filter.sample_ids
        obj._labels = msg.filter.labels
        obj._time_window_start_at_millis = msg.filter.time_window_start_at_millis
        obj._time_window_end_at_millis = msg.filter.time_window_end_at_millis
        obj._created_at_after_millis = msg.filter.created_at_after_millis
        obj._page_number = msg.page_number
        obj._page_limit = pagination_utils.page_limit_from_proto(msg.page_limit)

        return obj

    def _to_proto_request(self):
        return FindSummarySampleRequest(
            filter=FilterQuerySummarySample(
                find_summaries=self._find_summaries,
                sample_ids=self._sample_ids,
                labels=self._labels,
                time_window_start_at_millis=self._time_window_start_at_millis,
                time_window_end_at_millis=self._time_window_end_at_millis,
                created_at_after_millis=self._created_at_after_millis,
            ),
            page_number=self._page_number,
            page_limit=pagination_utils.page_limit_to_proto(self._page_limit),
        )

    def __repr__(self):
        return "SummarySampleQuery({})".format(self._to_proto_request())


class Summary(entity._ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(Summary, self).__init__(conn, conf, SummaryService, "summary", msg)
        self._conn = conn
        self._conf = conf
        self.monitored_entity_id = msg.monitored_entity_id
        self.name = msg.name
        self.type = msg.type_name

    def __repr__(self):
        return "Summary name:{}, type:{}, monitored_entity_id:{}".format(self.name, self.type, self.monitored_entity_id)

    def log_sample(self, data, labels, time_window_start, time_window_end, created_at=None):
        if not isinstance(data, data_types._VertaDataType):
            raise TypeError("expected a supported VertaDataType, found {}".format(type(data)))
        if data._type_string() != self.type:
            raise TypeError("expected a {}, found {}".format(self.type, data._type_string()))


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
        samples = [SummarySample(self._conn, self._conf, record) for record in success.samples]
        return samples

    @staticmethod
    def _labels_proto(labels):
        return {
            key: LabelFilterQuerySummarySample(label_value=values)
            for key, values in labels.items()
        }

    def delete(self, summary_records):
        try:
            ids = [record.id for record in summary_records]
        except:
            ids = summary_records
        endpoint = "/api/v1/summaries/deleteSample"
        msg = DeleteSummarySampleRequest(ids=ids)
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_proto_response(response, EmptyProto)
        return True


class SummarySample(entity._ModelDBEntity):
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
                "window: [{}, {})".format(self.time_window_start_at, self.time_window_end_at)
            )
        )

    @staticmethod
    def _maybe_deserialize_datatype(content):
        try:
            return data_types._VertaDataType._from_dict(json.loads(content))
        except:
            return content


class Summaries:
    def __init__(self, conn, conf):
        self._conn = conn
        self._conf = conf


    def create(self, name, data_type_cls, monitored_entity):
        assert issubclass(data_type_cls, data_types._VertaDataType)
        msg = CreateSummaryRequest(
            monitored_entity_id=monitored_entity.id,
            name=name,
            type_name=data_type_cls._type_string(),
        )
        endpoint = "/api/v1/summaries/createSummary"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        proto = self._conn.must_proto_response(response, SummaryProto)
        return Summary(self._conn, self._conf, proto)

    def find(self, query=None):
        if query is None:
            query = SummaryQuery()
        msg = query._to_proto_request()
        endpoint = "/api/v1/summaries/findSummary"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        maybe_summaries = self._conn.maybe_proto_response(
            response, FindSummaryRequest.Response
        )
        maybe_summaries = [Summary(self._conn, self._conf, msg) for msg in maybe_summaries.summaries]
        return maybe_summaries

    def delete(self, summaries):
        summary_ids = [summary.id for summary in summaries]
        msg = DeleteSummaryRequest(ids=summary_ids)
        endpoint = "/api/v1/summaries/deleteSummary"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_proto_response(response, EmptyProto)
        return True


class SummarySamples:
    def __init__(self, conn, conf):
        # TODO: potentially summary_id
        self._conn = conn
        self._conf = conf

    # TODO: potentially create()

    def find(self, query=None):
        if query is None:
            query = SummarySampleQuery()
        msg = query._to_proto_request()
        endpoint = "/api/v1/summaries/findSample"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        maybe_samples = self._conn.must_proto_response(response, msg.Response)
        return [
            SummarySample(self._conn, self._conf, sample)
            for sample in maybe_samples.samples
        ]

    def delete(self, summaries):
        summary_ids = extract_ids(summaries)
        msg = DeleteSummarySampleRequest(ids=summary_ids)
        endpoint = "/api/v1/summaries/deleteSample"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True
