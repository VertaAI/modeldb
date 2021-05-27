# -*- coding: utf-8 -*-

import hypothesis.strategies as st
from datetime import timedelta

from verta._internal_utils.time_utils import timedelta_millis


millis_timedelta_strategy = st.timedeltas(min_value=timedelta(milliseconds=1))

millis_uint64_strategy = millis_timedelta_strategy.map(timedelta_millis)
