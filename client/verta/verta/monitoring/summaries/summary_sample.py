# -*- coding: utf-8 -*-
"""A snapshot or window of data."""

from __future__ import print_function

import json

from verta._internal_utils import time_utils
from verta._protos.public.monitoring import Summary_pb2 as SummaryService
from verta.tracking.entities import _entity
from verta import data_types


class SummarySample(_entity._ModelDBEntity):
    """A summary sample object capturing data for later comparison.

    Users should obtain summary samples through one of the query or create
    methods on a :class:`Summary` or the ``summary_samples`` attribute on the
    monitoring sub-:class:`~verta.monitoring.client.Client` instead
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
        A :mod:`VertaDataType <verta.data_types>` consistent with the type of this summary.
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
