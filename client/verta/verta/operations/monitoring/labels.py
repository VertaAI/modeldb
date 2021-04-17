# -*- coding: utf-8 -*-


from verta._protos.public.monitoring.Summary_pb2 import FilterQuerySummarySample

from verta._protos.public.monitoring.Labels_pb2 import (
    FindSampleLabelsRequest,
    FindSampleLabelValuesItem,
    FindSampleLabelValuesRequest,
)
from .summaries import Summary
from verta._internal_utils import time_utils
from .utils import maybe

class Labels:
    def __init__(self, conn, conf):
        self._conn = conn
        self._conf = conf


    def find_keys(self, **kwargs):
        """
            Find a list of labels according to provided parameters.

            :key summary_query:
            :key sample_ids:
            :key labels:
            :key time_window_start:
            :key time_window_end:
            :return: find_keys should return a list of strings used as label keys
            :rtype: list
        """
        summary_filter = self._build_summary_filter(**kwargs)
        msg = FindSampleLabelsRequest(
            filter=summary_filter, page_number=1, page_limit=-1,
        )
        endpoint = "/api/v1/labels/findLabels"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        proto = self._conn.must_proto_response(
            response, FindSampleLabelsRequest.Response
        )
        return [label for label in proto.labels]

    def find_values(self, **kwargs):
        """
            Find a dictionary of label keys and values according to provided parameters.

            :key summary_query:
            :key sample_ids:
            :key labels:
            :key time_window_start:
            :key time_window_end:
            :key keys: the label keys for which values should be returned
            :return: find_keys should return a list of strings used as label keys
            :rtype: list
        """
        summary_filter = self._build_summary_filter(**kwargs)
        if 'keys' in kwargs:
            keys = kwargs['keys']
        else:
            keys = []
        msg = FindSampleLabelValuesRequest(
            filter=summary_filter, labels=keys,
            page_number=1, page_limit=-1,
        )
        endpoint = "/api/v1/labels/findLabelValues"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        proto = self._conn.must_proto_response(
            response, FindSampleLabelValuesRequest.Response
        )
        return {
            key: set(valuesItem.values)
            for key, valuesItem in proto.labels.items()
        }


    def _build_summary_filter(self, **kwargs):
        summary_query = kwargs.get('summary_query', None)
        summaries_proto = maybe(lambda q: q._to_proto_request(), summary_query)

        sample_ids = kwargs.get('sample_id', None)

        window_start = kwargs.get('time_window_start', None)
        window_start_at_millis = maybe(lambda t: time_utils.epoch_millis(t), window_start)

        window_end = kwargs.get('time_window_end', None)
        window_end_at_millis = maybe(lambda t: time_utils.epoch_millis(t), window_end)

        labels = kwargs.get('labels', None)
        labels_proto = maybe(Summary._labels_proto, labels)
        return FilterQuerySummarySample(
            find_summaries=summaries_proto,
            sample_ids=sample_ids,
            labels=labels_proto,
            time_window_start_at_millis=window_start_at_millis,
            time_window_end_at_millis=window_end_at_millis,
        )
