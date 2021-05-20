import contextlib
import copy
import filecmp
import random
import os
import sys
from string import printable

import requests

from verta._internal_utils import _utils
from verta._protos.public.uac import Organization_pb2 as _OrganizationService

from hypothesis import strategies as st


def gen_none():
    return None


def gen_bool():
    return random.random() > .5


def gen_float(start=1, stop=None):
    if stop is None:
        return random.random()*start
    else:
        return random.uniform(start, stop)


def gen_int(start=10, stop=None):
    return random.randrange(start, stop)


def gen_str(length=8):
    return ''.join([chr(random.randrange(97, 123))
                    for _
                    in range(length)])


def gen_list(length=8):
    """Generates a list with mixed-type elements."""
    gen_el = lambda fns=(gen_none, gen_bool, gen_float, gen_int, gen_str): random.choice(fns)()
    return [gen_el() for _ in range(length)]


def gen_dict(length=8):
    """Generates a single-level dict with string keys and mixed-type values."""
    gen_val = lambda fns=(gen_none, gen_bool, gen_float, gen_int, gen_str): random.choice(fns)()
    res = {}
    while len(res) < length:
        res[gen_str()] = gen_val()
    return res


@st.composite
def st_scalars(draw):
    # pylint: disable=bad-continuation
    return draw(st.none()
              | st.booleans()
              | st.integers()
              | st.floats(allow_nan=False, allow_infinity=False)
              | st.text(printable))


@st.composite
def st_json(draw, max_size=6):
    # pylint: disable=bad-continuation
    return draw(st.recursive(st_scalars(),
                             lambda children: st.lists(children,
                                                       min_size=1,
                                                       max_size=max_size)
                                            | st.dictionaries(st.text(printable),
                                                              children,
                                                              min_size=1,
                                                              max_size=max_size)))


@st.composite
def st_keys(draw):
    return draw(st.text(sorted(_utils._VALID_FLAT_KEY_CHARS), min_size=1))


@st.composite
def st_key_values(draw, min_size=1, max_size=12, scalars_only=False):
    return draw(st.dictionaries(st_keys(),
                                st_scalars() if scalars_only else st_json(),
                                min_size=min_size,
                                max_size=max_size))


@contextlib.contextmanager
def chdir(new_dir):
    """
    Context manager for safely changing current working directory.

    Without this, if a test involving a directory change fails then subsequent tests would likely
    fail as well due to a bad execution state.

    """
    old_dir = os.getcwd()
    os.chdir(new_dir)
    try:
        yield
    finally:
        os.chdir(old_dir)


@contextlib.contextmanager
def sys_path_manager():
    """
    Context manager for safely modifying `sys.path`.

    Without this, if a test involving a `sys.path` modification fails then subsequent tests could
    possibly fail as well due to a bad execution state.

    """
    old_sys_path = copy.copy(sys.path)
    try:
        yield sys.path
    finally:
        sys.path = old_sys_path


def get_build_ids(status):
    # get the set of build_ids in the status of the stage:
    return set(map(lambda comp: comp["build_id"], status["components"]))


def assert_dirs_match(dir1, dir2):
    assert os.path.isdir(dir1)
    assert os.path.isdir(dir2)

    dircmp = filecmp.dircmp(dir1, dir2)
    assert not dircmp.diff_files
    assert not dircmp.left_only
    assert not dircmp.right_only
