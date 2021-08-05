# -*- coding: utf-8 -*-

from datetime import timedelta
import os
import string

import hypothesis.strategies as st
from verta._internal_utils.time_utils import duration_millis
import warnings


def duration_millis_ignore_warn(delta):
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        return duration_millis(delta)


millis_timedelta_strategy = st.timedeltas(min_value=timedelta(milliseconds=1))

millis_uint64_strategy = millis_timedelta_strategy.map(duration_millis_ignore_warn)


@st.composite
def filepath(draw):
    """A valid filepath. Does **not** create the file.

    Returns
    -------
    str

    """
    # https://stackoverflow.com/q/4814040#comment38006480_4814088
    # TODO: add more characters; macOS and Windows allow swaths of Unicode
    legal_chars = string.ascii_letters + string.digits + "._-"
    num_segments = draw(st.integers(min_value=1, max_value=6))

    segments = [
        draw(st.text(legal_chars, min_size=1))
        for _ in range(num_segments)
    ]
    return os.path.join(*segments)
