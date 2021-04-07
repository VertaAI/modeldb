# -*- coding: utf-8 -*-

from __future__ import print_function

import json
from datetime import datetime
from verta._internal_utils import time_utils
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
    def __init__(self, ids=None, names=None, type_names=None, monitored_entities=None):
        self._ids = extract_ids(ids) if ids else None
        self._names = names
        self._type_names = type_names
        self._monitored_entity_ids = (
            extract_ids(monitored_entities) if monitored_entities else None
        )

    @classmethod
    def _from_proto_request(cls, msg):
        return cls(
            ids=msg.ids,
            names=msg.names,
            type_names=msg.type_names,
            monitored_entities=msg.monitored_entity_ids,
        )

    def _to_proto_request(self):
        return FindSummaryRequest(
            ids=self._ids,
            names=self._names,
            type_names=self._type_names,
            monitored_entity_ids=self._monitored_entity_ids,
        )

    def __repr__(self):
        return "SummaryQuery({}, {}, {}, {})".format(self._ids, self._names, self._type_names, self._monitored_entity_ids)


class SummarySampleQuery(object):
    def __init__(self, summary_query=None, ids=None, labels=None, time_window_start_at_millis=None, time_window_end_at_millis=None):
        self._find_summaries = summary_query._to_proto_request() if summary_query else None
        self._sample_ids = extract_ids(ids) if ids else None
        self._labels = maybe(Summary._labels_proto, labels)
        self._time_window_start_at_millis = time_window_start_at_millis
        self._time_window_end_at_millis = time_window_end_at_millis

    @classmethod
    def _from_proto_request(cls, msg):
        # set attrs after creation to bypass conversion logic in __init__()
        print(msg)
        obj = cls()
        obj._find_summaries = msg.filter.find_summaries
        obj._sample_ids = msg.filter.sample_ids
        obj._labels = msg.filter.labels
        obj._time_window_start_at_millis = msg.filter.time_window_start_at_millis
        obj._time_window_end_at_millis = msg.filter.time_window_end_at_millis

        return obj

    def _to_proto_request(self):
        return FindSummarySampleRequest(
            filter=FilterQuerySummarySample(
                find_summaries=self._find_summaries,
                sample_ids=self._sample_ids,
                labels=self._labels,
                time_window_start_at_millis=self._time_window_start_at_millis,
                time_window_end_at_millis=self._time_window_end_at_millis,
            )
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
        if not created_at:
            created_at = time_utils.now()

        if isinstance(data, data_types._VertaDataType):
            content = json.dumps(data._as_dict())
        else:
            content = data

        created_at_millis = time_utils.epoch_millis(created_at)
        window_start_millis = time_utils.epoch_millis(time_window_start)
        window_end_millis = time_utils.epoch_millis(time_window_end)

        msg = CreateSummarySample(
            summary_id=self.id,
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

    def find_samples(
        self,
        sample_ids=None,
        labels=None,
        time_window_start=None,
        time_window_end=None,
    ):
        summaries_proto = SummaryQuery(ids=[self.id])._to_proto_request()
        time_window_start_at_millis = time_utils.epoch_millis(time_window_start) if time_window_start else None
        time_window_end_at_millis = time_utils.epoch_millis(time_window_end) if time_window_end else None
        labels_proto = Summary._labels_proto(labels) if labels else None
        filterSamples = FilterQuerySummarySample(
            find_summaries=summaries_proto,
            sample_ids=sample_ids,
            labels=labels_proto,
            time_window_start_at_millis=time_window_start_at_millis,
            time_window_end_at_millis=time_window_end_at_millis,
        )

        msg = FindSummarySampleRequest(filter=filterSamples)
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

    def create(self, name, type, monitored_entity):
        msg = CreateSummaryRequest(
            monitored_entity_id=monitored_entity.id,
            name=name,
            type_name=type,
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
