# -*- coding: utf-8 -*-


from _protos.private.monitoring.Summary_pb2 import FilterQuerySummarySample

from _protos.private.monitoring.Labels_pb2 import (
    FindSampleLabelsRequest,
    FindSampleLabelValuesItem,
    FindSampleLabelValuesRequest,
)
from clients.summaries import Summary
import time_utils


class Labels:
    def __init__(self, conn, conf):
        self._conn = conn
        self._conf = conf

    def find(
        self,
        summary_query=None,
        sample_ids=None,
        labels=None,
        time_window_start=None,
        time_window_end=None,
        keys=None,
    ):
        summaries_proto = summary_query._to_proto_request() if summary_query else None
        time_window_start_at_millis = time_utils.epoch_millis(time_window_start) if time_window_start else None
        time_window_end_at_millis = time_utils.epoch_millis(time_window_end) if time_window_end else None
        labels_proto = Summary._labels_proto(labels) if labels else None
        summary_filter = FilterQuerySummarySample(
            find_summaries=summaries_proto,
            sample_ids=sample_ids,
            labels=labels_proto,
            time_window_start_at_millis=time_window_start_at_millis,
            time_window_end_at_millis=time_window_end_at_millis,
        )
        if keys:
            return self._find_keys_and_values(summary_filter, keys)
        else:
            return self._find_keys_only(summary_filter)


    def _find_keys_only(self, summary_filter):
        msg = FindSampleLabelsRequest(filter=summary_filter)
        endpoint = "/api/v1/labels/findLabels"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        proto = self._conn.must_proto_response(
            response, FindSampleLabelsRequest.Response
        )
        return [label for label in proto.labels]

    def _find_keys_and_values(self, summary_filter, keys):
        msg = FindSampleLabelValuesRequest(filter=summary_filter, labels=keys)
        endpoint = "/api/v1/labels/findLabelValues"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        proto = self._conn.must_proto_response(
            response, FindSampleLabelValuesRequest.Response
        )
        return {
            key: {v for v in valuesItem.values}
            for key, valuesItem in proto.labels.items()
        }
