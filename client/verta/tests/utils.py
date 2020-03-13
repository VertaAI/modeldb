import contextlib
import random
import os
from string import printable

import requests

from verta._internal_utils import _utils

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


def delete_project(id_, conn):
    request_url = "{}://{}/api/v1/modeldb/project/deleteProject".format(conn.scheme, conn.socket)
    response = requests.delete(request_url, json={'id': id_}, headers=conn.auth)
    response.raise_for_status()


def delete_experiment(id_, conn):
    request_url = "{}://{}/api/v1/modeldb/experiment/deleteExperiment".format(conn.scheme, conn.socket)
    response = requests.delete(request_url, json={'id': id_}, headers=conn.auth)
    response.raise_for_status()


def delete_experiment_run(id_, conn):
    request_url = "{}://{}/api/v1/modeldb/experiment-run/deleteExperimentRun".format(conn.scheme, conn.socket)
    response = requests.delete(request_url, json={'id': id_}, headers=conn.auth)
    response.raise_for_status()

def delete_datasets(ids, conn):
    request_url = "{}://{}/api/v1/modeldb/dataset/deleteDatasets".format(conn.scheme, conn.socket)
    response = requests.delete(request_url, json={'ids': ids}, headers=conn.auth)
    response.raise_for_status()

def delete_repository(id_, conn):
    request_url = "{}://{}/api/v1/modeldb/versioning/repositories/{}".format(conn.scheme, conn.socket, id_)
    response = requests.delete(request_url, headers=conn.auth)
    response.raise_for_status()

def delete_commit(repo_id, id_, conn):
    request_url = "{}://{}/api/v1/modeldb/versioning/repositories/{}/commits/{}".format(conn.scheme, conn.socket, repo_id, id_)
    response = requests.delete(request_url, headers=conn.auth)
    response.raise_for_status()
