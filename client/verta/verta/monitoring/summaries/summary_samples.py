# -*- coding: utf-8 -*-
"""A collection of summary samples."""

from __future__ import print_function


from verta._protos.public.monitoring.Summary_pb2 import DeleteSummarySampleRequest
from verta._internal_utils import arg_handler
from .queries import SummarySampleQuery
from .summary_sample import SummarySample


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
        summary_samples = client.monitoring.summary_samples

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
        query : :class:`~verta.monitoring.summaries.queries.SummarySampleQuery`, optional
            A query object which filters the set of summary samples.

        Returns
        -------
        list of :class:`~verta.monitoring.summaries.summary_sample.SummarySample`
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
        summary_samples : list of :class:`~verta.monitoring.summaries.summary_sample.SummarySample`
            The summary samples which should be deleted from this summary.

        Returns
        -------
        bool
            True if the delete was successful.
        """
        summary_sample_ids = arg_handler.extract_ids(summary_samples)
        msg = DeleteSummarySampleRequest(ids=summary_sample_ids)
        endpoint = "/api/v1/summaries/deleteSample"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True
