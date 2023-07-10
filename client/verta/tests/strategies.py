# -*- coding: utf-8 -*-

from datetime import timedelta
import os
import re
import string
import warnings

import hypothesis
import hypothesis.strategies as st
from hypothesis import given

from tests.registry.pydantic_models import AnInnerClass, InputClass, OutputClass
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
def filepath(draw, allow_parent_dir_segments=False):
    """A valid filepath. Does **not** create the file.

    Parameters
    ----------
    allow_parent_dir_segments : bool, default False
        Whether to allow paths that contain `".."` segments.

    Returns
    -------
    str

    """
    # https://stackoverflow.com/q/4814040#comment38006480_4814088
    # TODO: add more characters; macOS and Windows allow swaths of Unicode
    legal_chars = string.ascii_letters + string.digits + "._-"
    num_segments = draw(st.integers(min_value=1, max_value=6))

    segments = [draw(st.text(legal_chars, min_size=1)) for _ in range(num_segments)]
    if not allow_parent_dir_segments:
        hypothesis.assume(".." not in segments)

    return os.path.join(*segments)


@st.composite
def env_vars(draw):
    """For use as Environment versioning's `env_var` parameter.

    The parameter can be either a list of variable names that exist in the
    environment or a mapping of names to specific values.

    Returns
    -------
    env_vars : list of str, or dict of str to str
        Environment variables.

    Examples
    --------
    .. code-block:: python

        env = Environment(env_vars=env_vars)

        if isinstance(env_vars, list):
            env_vars = {name: os.environ[name] for name in env_vars}

        assert env.env_vars == (env_vars or None)

    """
    return draw(
        st.one_of(
            st.lists(
                st.sampled_from(sorted(os.environ.keys())),
            ),
            st.dictionaries(
                keys=st.text(min_size=1),
                values=st.text(min_size=1),
            ),
        )
    )


@st.composite
def generate_inner_object(draw):
    h_dict = draw(st.dictionaries(st.text(), st.integers()))
    i_list_str = draw(st.lists(st.text()))
    return AnInnerClass(
        h_dict=h_dict,
        i_list_str=i_list_str,
    )


@st.composite
def generate_object(draw):
    a_int = draw(st.integers())
    b_str = draw(st.text())
    c_float = draw(st.floats())
    d_bool = draw(st.booleans())
    e_list_int = draw(st.lists(st.integers()))
    f_dict = draw(st.dictionaries(st.text(), st.text()))
    g_inner_input_class = draw(generate_inner_object())

    return InputClass(
        a_int=a_int,
        b_str=b_str,
        c_float=c_float,
        d_bool=d_bool,
        e_list_int=e_list_int,
        f_dict=f_dict,
        g_inner=g_inner_input_class,
    )


@st.composite
def generate_another_object(draw):
    j_bool = draw(st.booleans())
    k_list_list_int = draw(st.lists(st.lists(st.integers())))
    l_str = draw(st.text())

    return OutputClass(
        j_bool=j_bool,
        k_list_list_int=k_list_list_int,
        l_str=l_str,
    )
