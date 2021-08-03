# -*- coding: utf-8 -*-

from datetime import timedelta
from string import printable
import warnings

import hypothesis.strategies as st
from verta._internal_utils.time_utils import duration_millis


def duration_millis_ignore_warn(delta):
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        return duration_millis(delta)


millis_timedelta_strategy = st.timedeltas(min_value=timedelta(milliseconds=1))

millis_uint64_strategy = millis_timedelta_strategy.map(duration_millis_ignore_warn)

# from https://hypothesis.readthedocs.io/en/latest/data.html#recursive-data
json_strategy = st.recursive(
    st.none() | st.booleans() | st.floats() | st.text(printable),
    lambda children: st.lists(children, 1)
    | st.dictionaries(st.text(printable), children, min_size=1),
)
