# -*- coding: utf-8 -*-

import hypothesis
import hypothesis.strategies as st
import pytest
from verta._protos.public.monitoring.Summary_pb2 import AggregationQuerySummary
from verta.monitoring.summaries.aggregation import Aggregation
from ..time_strategies import millis_timedelta_strategy, millis_uint64_strategy

ProtoOperations = AggregationQuerySummary.AggregationOperation

aggregation_ops_strategy = st.sampled_from(ProtoOperations.values())
aggregation_query_strategy = st.builds(
    AggregationQuerySummary,
    time_granularity_millis=millis_uint64_strategy,
    operation=aggregation_ops_strategy,
)

granularity_strategy = st.one_of(
    st.sampled_from(["30m", "1d"]), millis_timedelta_strategy, millis_uint64_strategy
)

op_str_strategy = st.one_of(
    st.sampled_from(ProtoOperations.values()),
    st.sampled_from(ProtoOperations.keys()),
    st.sampled_from(Aggregation.operations()),
)

ops_strategy = st.one_of(
    aggregation_ops_strategy,
    st.sampled_from(AggregationQuerySummary.AggregationOperation.keys()),
)


@st.composite
def series(draw, num_rows, data_type, name=None, has_missing=True):
    """Generate Series.

    Parameters
    ----------
    num_rows : int
    data_type : str
        ``"continuous"`` or ``"discrete"``.
    name : str, optional
    has_missing : bool, default True
        Whether to sprinkle missing values into the series.

    Returns
    -------
    pd.Series

    """
    pd = pytest.importorskip("pandas")

    value_strategies = []
    if has_missing:
        value_strategies.append(st.just(None))
    if data_type == "continuous":
        value_strategies.extend(
            [
                st.floats(
                    # NumPy has limits on what values it can handle for histograms
                    min_value=-(2 ** 32),
                    max_value=2 ** 32,
                    allow_nan=False,
                    allow_infinity=False,
                ),
            ]
        )
    elif data_type == "discrete":
        value_strategies.extend(
            [
                st.integers(),
                st.text(),
            ]
        )
    else:
        raise ValueError("invalid data type {}".format(data_type))

    values = draw(
        st.lists(
            st.one_of(value_strategies),
            min_size=num_rows,
            max_size=num_rows,
        )
    )
    return pd.Series(values, name=name)


@st.composite
def dataframes(draw):
    """Generate DataFrames with a "continuous" and "discrete" column.

    Returns
    -------
    pd.DataFrame

    """
    pd = pytest.importorskip("pandas")

    # Hypothesis suggests a limit on example sizes
    num_rows = draw(st.integers(min_value=1, max_value=2 ** 8))

    continuous_col = "continuous"
    discrete_col = "discrete"

    continuous_series = draw(series(num_rows, continuous_col, continuous_col))
    discrete_series = draw(series(num_rows, discrete_col, discrete_col))

    # our profiler heuristics assume <= 20 unique values is discrete
    hypothesis.assume(len(set(continuous_series)) > 20)

    return pd.DataFrame(
        {
            continuous_col: continuous_series,
            discrete_col: discrete_series,
        }
    )
