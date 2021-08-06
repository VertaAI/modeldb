# -*- coding: utf-8 -*-

from datetime import timedelta
import os
import string
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
    st.none() | st.booleans() | st.floats() | st.text(string.printable),
    lambda children: st.lists(children)
    | st.dictionaries(st.text(string.printable), children),
    max_leaves=500,
)


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

    segments = [draw(st.text(legal_chars, min_size=1)) for _ in range(num_segments)]
    return os.path.join(*segments)
