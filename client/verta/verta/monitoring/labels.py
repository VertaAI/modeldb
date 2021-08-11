# -*- coding: utf-8 -*-
"""Summary labels."""

from verta._protos.public.monitoring.Summary_pb2 import FilterQuerySummarySample

from verta._protos.public.monitoring.Labels_pb2 import (
    FindSampleLabelsRequest,
    FindSampleLabelValuesRequest,
)
from .summaries.queries import _labels_proto
from verta._internal_utils import arg_handler, time_utils


class Labels(object):
    """
    Collection object for finding labels.

    A label is a key-value pair associated with a summary.

    Examples
    --------
    .. code-block:: python

        from verta import Client

        client = Client()
        labels = client.monitoring.labels

    """

    def __init__(self, conn, conf):
        self._conn = conn
        self._conf = conf

    def find_keys(self, **kwargs):
        """
        Find a list of labels according to provided parameters.

        Uses the supplied arguments to filter down the set of summaries inspected,
        and returns as a list the set of label keys which exist for that set of
        summaries.

        Parameters
        ----------
        summary_query : :class:`~verta.monitoring.summaries.queries.SummaryQuery`, optional
            A query object specifying a set of summaries.
        sample_ids : list, optional
             A list of integer sample ids.
        labels : dict, optional
             A dictionary from label key strings to either label value strings
             or a list of label value strings.
        time_window_start : datetime.datetime or int
            Either a timezone aware datetime object or unix
            epoch milliseconds.
        time_window_end : datetime.datetime or int
            Either a timezone aware datetime object or unix
            epoch milliseconds.

        Returns
        -------
        list
            A list of strings used as label keys.
        """
        summary_filter = self._build_summary_filter(**kwargs)
        msg = FindSampleLabelsRequest(
            filter=summary_filter,
            page_number=1,
            page_limit=-1,
        )
        endpoint = "/api/v1/labels/findLabels"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        proto = self._conn.must_proto_response(
            response, FindSampleLabelsRequest.Response
        )
        return [label for label in proto.labels]

    def find_values(self, **kwargs):
        """
        Find the values of specified label keys according to provided parameters.

        Uses the supplied arguments to filter down the set of summaries inspected,
        and returns the list of label values for the specified list of label keys in
        a dictionary.

        Parameters
        ----------
        keys : list
            A list of strings specifying the label keys to retrieve label values
            for.
        summary_query : :class:`~verta.monitoring.summaries.queries.SummaryQuery`, optional
            A query object specifying a set of summaries.
        sample_ids : list, optional
             A list of integer sample ids.
        labels : dict, optional
             A dictionary from label key strings to either label value strings
             or a list of label value strings.
        time_window_start : datetime.datetime or int
            Either a timezone aware datetime object or unix
            epoch milliseconds.
        time_window_end : datetime.datetime or int
            Either a timezone aware datetime object or unix
            epoch milliseconds.

        Returns
        -------
        dict
            A dictionary from label key strings to sets of string values.
        """
        summary_filter = self._build_summary_filter(**kwargs)
        if "keys" in kwargs:
            keys = kwargs["keys"]
        else:
            keys = []
        msg = FindSampleLabelValuesRequest(
            filter=summary_filter,
            labels=keys,
            page_number=1,
            page_limit=-1,
        )
        endpoint = "/api/v1/labels/findLabelValues"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        proto = self._conn.must_proto_response(
            response, FindSampleLabelValuesRequest.Response
        )
        return {key: set(valuesItem.values) for key, valuesItem in proto.labels.items()}

    def _build_summary_filter(self, **kwargs):
        summary_query = kwargs.get("summary_query", None)
        summaries_proto = arg_handler.maybe(lambda q: q._to_proto_request(), summary_query)

        sample_ids = kwargs.get("sample_id", None)

        window_start = kwargs.get("time_window_start", None)
        window_start_at_millis = arg_handler.maybe(
            lambda t: time_utils.epoch_millis(t), window_start
        )

        window_end = kwargs.get("time_window_end", None)
        window_end_at_millis = arg_handler.maybe(lambda t: time_utils.epoch_millis(t), window_end)

        labels = kwargs.get("labels", None)
        labels_proto = arg_handler.maybe(_labels_proto, labels)
        return FilterQuerySummarySample(
            find_summaries=summaries_proto,
            sample_ids=sample_ids,
            labels=labels_proto,
            time_window_start_at_millis=window_start_at_millis,
            time_window_end_at_millis=window_end_at_millis,
        )
