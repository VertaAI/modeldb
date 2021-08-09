# -*- coding: utf-8 -*-

from verta.monitoring.summaries.queries import SummaryQuery, SummarySampleQuery


class TestSummarySampleQuery:
    def test_empty_query(self):
        empty_query = SummarySampleQuery()
        empty_to_proto = empty_query._to_proto_request()
        empty_from_proto = SummarySampleQuery._from_proto_request(empty_to_proto)
        assert empty_query == empty_from_proto

        # ensure that the aggregation submessage isn't set when not provided,
        # which has caused backend errors in the past
        assert not empty_to_proto.HasField("aggregation")
        assert empty_from_proto.aggregation is None
