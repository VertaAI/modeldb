# -*- coding: utf-8 -*-

from datetime import timedelta

import hypothesis.strategies as st
from verta._internal_utils.time_utils import duration_millis
import warnings


def duration_millis_ignore_warn(delta):
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        return duration_millis(delta)


millis_timedelta_strategy = st.timedeltas(min_value=timedelta(milliseconds=1))

millis_uint64_strategy = millis_timedelta_strategy.map(duration_millis_ignore_warn)
