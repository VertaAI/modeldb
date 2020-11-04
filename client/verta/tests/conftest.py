from __future__ import division

import six

import datetime
import os
import random
import shutil
import string
import tempfile
import subprocess
import sys

import requests

import verta
from verta import Client
from verta._internal_utils import _utils

import hypothesis
import pytest
from . import utils


RANDOM_SEED = 0
INPUT_LENGTH = 12  # length of iterable input fixture

DEFAULT_HOST = None
DEFAULT_PORT = None
DEFAULT_EMAIL = None
DEFAULT_DEV_KEY = None


# hypothesis on Jenkins is apparently too slow
hypothesis.settings.register_profile("default", suppress_health_check=[hypothesis.HealthCheck.too_slow])
hypothesis.settings.load_profile("default")


def pytest_addoption(parser):
    parser.addoption("--oss", action="store_true", help="run OSS-compatible tests")


def pytest_collection_modifyitems(config, items):
    if config.getoption("--oss"):
        skip_not_oss = pytest.mark.skip(reason="not available in OSS")
        for item in items:
            if 'not_oss' in item.keywords:
                item.add_marker(skip_not_oss)
    else:
        skip_oss = pytest.mark.skip(reason="only applicable to OSS")
        for item in items:
            if 'oss' in item.keywords:
                item.add_marker(skip_oss)


@pytest.fixture(scope='session')
def host():
    return os.environ.get("VERTA_HOST", DEFAULT_HOST)


@pytest.fixture(scope='session')
def port():
    return os.environ.get("VERTA_PORT", DEFAULT_PORT)


@pytest.fixture(scope='session')
def email():
    return os.environ.get("VERTA_EMAIL", DEFAULT_EMAIL)


@pytest.fixture(scope='session')
def dev_key():
    return os.environ.get("VERTA_DEV_KEY", DEFAULT_DEV_KEY)


@pytest.fixture(scope='session')
def email_2():
    # For collaboration tests
    return os.environ.get("VERTA_EMAIL_2")


@pytest.fixture(scope='session')
def dev_key_2():
    # For collaboration tests
    return os.environ.get("VERTA_DEV_KEY_2")


@pytest.fixture
def seed():
    return RANDOM_SEED


@pytest.fixture
def nones():
    return [None]*INPUT_LENGTH


@pytest.fixture
def bools(seed):
    random.seed(seed)
    return [bool(random.randint(0, 1)) for _ in range(INPUT_LENGTH)]


@pytest.fixture
def floats(seed):
    random.seed(seed)
    return [random.uniform(-3**2, 3**3) for _ in range(INPUT_LENGTH)]


@pytest.fixture
def ints(seed):
    random.seed(seed)
    return [random.randint(-3**4, 3**5) for _ in range(INPUT_LENGTH)]


@pytest.fixture
def strs(seed):
    """no duplicates"""
    random.seed(seed)
    gen_str = lambda: ''.join(random.choice(string.ascii_letters) for _ in range(INPUT_LENGTH))
    result = set()
    while len(result) < INPUT_LENGTH:
        single_str = gen_str()
        while single_str in result:
            single_str = gen_str()
        else:
            result.add(single_str)
    return list(result)


@pytest.fixture
def flat_lists(seed, nones, bools, floats, ints, strs):
    random.seed(seed)
    values = (nones, bools, floats, ints, strs)
    return [
        [
            values[random.choice(range(len(values)))][i]
            for i in range(INPUT_LENGTH)
        ]
        for _ in range(INPUT_LENGTH)
    ]


@pytest.fixture
def flat_dicts(seed, nones, bools, floats, ints, strs):
    random.seed(seed)
    values = (nones, bools, floats, ints, strs)
    return [
        {
            strs[i]: values[random.choice(range(len(values)))][i]
            for i in range(INPUT_LENGTH)
        }
        for _ in range(INPUT_LENGTH)
    ]


@pytest.fixture
def nested_lists(seed, nones, bools, floats, ints, strs):
    random.seed(seed)
    values = (nones, bools, floats, ints, strs)
    flat_values = [value for type_values in values for value in type_values]
    def gen_value(p=1):
        if random.random() < p:
            return [
                gen_value(p/2)
                for _ in range(random.choice(range(4)))
            ]
        else:
            return random.choice(flat_values)
    return [
        [
            gen_value()
            for _ in range(random.choice(range(3))+1)
        ]
        for _ in range(INPUT_LENGTH)
    ]


@pytest.fixture
def nested_dicts(seed, nones, bools, floats, ints, strs):
    random.seed(seed)
    values = (nones, bools, floats, ints, strs)
    flat_values = [value for type_values in values for value in type_values]
    def gen_value(p=1):
        if random.random() < p:
            return {
                key: gen_value(p/2)
                for key, _ in zip(strs, range(random.choice(range(4))))
            }
        else:
            return random.choice(flat_values)
    return [
        {
            key: gen_value()
            for key, _ in zip(strs, range(random.choice(range(3))+1))
        }
        for _ in range(INPUT_LENGTH)
    ]


@pytest.fixture
def scalar_values(nones, bools, floats, ints, strs):
    return [type_values[0]
            for type_values in (nones, bools, floats, ints, strs)]


@pytest.fixture
def collection_values(flat_lists, flat_dicts, nested_lists, nested_dicts):
    return [type_values[0]
            for type_values in (flat_lists, flat_dicts, nested_lists, nested_dicts)]


@pytest.fixture
def all_values(scalar_values, collection_values):
    return scalar_values + collection_values


@pytest.fixture(scope='session')
def output_path():
    dirpath = ".outputs"
    while len(dirpath) < 1024:
        try:  # avoid name collisions
            os.mkdir(dirpath)
        except OSError:
            dirpath += '_'
        else:
            yield os.path.join(dirpath, "{}")
            break
    else:
        raise RuntimeError("dirpath length exceeded 1024")
    shutil.rmtree(dirpath)


@pytest.fixture
def dir_and_files(strs, tmp_path):
    """
    Creates nested directory of empty files.

    Returns
    -------
    dirpath : str
    filepaths : set of str

    """
    filepaths = {
        os.path.join(strs[0], strs[1], strs[2]),
        os.path.join(strs[0], strs[1], strs[3]),
        os.path.join(strs[0], strs[2]),
        os.path.join(strs[0], strs[4]),
        os.path.join(strs[2]),
        os.path.join(strs[5]),
    }

    for filepath in filepaths:
        p = tmp_path / filepath
        p.parent.mkdir(parents=True, exist_ok=True)
        p.touch()

    return str(tmp_path), filepaths


@pytest.fixture
def tempdir_root():
    return os.environ.get("TEMPDIR_ROOT")


@pytest.fixture
def in_tempdir(tempdir_root):
    """Moves test to execute inside a temporary directory."""
    dirpath = tempfile.mkdtemp(dir=tempdir_root)
    try:
        with utils.chdir(dirpath):
            yield dirpath
    finally:
        shutil.rmtree(dirpath)


@pytest.fixture
def client(host, port, email, dev_key):
    print("[TEST LOG] test setup begun {} UTC".format(datetime.datetime.utcnow()))
    client = Client(host, port, email, dev_key, debug=True)

    yield client

    proj = client._ctx.proj
    if proj is not None:
        proj.delete()

    print("[TEST LOG] test teardown completed {} UTC".format(datetime.datetime.utcnow()))


@pytest.fixture
def client_2(host, port, email_2, dev_key_2):
    """For collaboration tests."""
    if not (email_2 and dev_key_2):
        pytest.skip("second account credentials not present")
    print("[TEST LOG] test setup begun {} UTC".format(datetime.datetime.utcnow()))

    client = Client(host, port, email_2, dev_key_2, debug=True)

    yield client

    print("[TEST LOG] test teardown completed {} UTC".format(datetime.datetime.utcnow()))


@pytest.fixture
def experiment_run(client):
    proj = client.set_project()
    print("[TEST LOG] Project ID is {}".format(proj.id))
    client.set_experiment()
    run = client.set_experiment_run()
    print("[TEST LOG] Run ID is {}".format(run.id))

    return run


@pytest.fixture
def model_for_deployment(strs):
    np = pytest.importorskip("numpy")
    pd = pytest.importorskip("pandas")
    sklearn = pytest.importorskip("sklearn")
    from sklearn import linear_model

    num_rows, num_cols = 36, 6

    data = pd.DataFrame(np.tile(np.arange(num_rows).reshape(-1, 1),
                                num_cols),
                        columns=strs[:num_cols])
    X_train = data.iloc[:,:-1]  # pylint: disable=bad-whitespace
    y_train = data.iloc[:, -1]

    return {
        'model': sklearn.linear_model.LogisticRegression(),
        'model_api': verta.utils.ModelAPI(X_train, y_train),
        'requirements': six.StringIO("scikit-learn=={}".format(sklearn.__version__)),
        'train_features': X_train,
        'train_targets': y_train,
    }


@pytest.fixture
def repository(client):
    name = _utils.generate_default_name()
    repo = client.get_or_create_repository(name)

    yield repo

    repo.delete()


@pytest.fixture
def commit(repository):
    commit = repository.get_commit()

    yield commit


@pytest.fixture
def created_datasets(client):
    """Container to track and clean up Datasets created during tests."""
    created_datasets = []

    yield created_datasets

    datasets_conn = {}  # prevent duplication
    for dataset in created_datasets:
        datasets_conn[dataset.id] = dataset._conn

    for dataset_id in datasets_conn:
        utils.delete_datasets([dataset_id], datasets_conn[dataset_id])


@pytest.fixture
def registered_model(client):
    model = client.get_or_create_registered_model()
    yield model
    model.delete()


@pytest.fixture
def created_registered_models():
    """Container to track and clean up `RegisteredModel`s created during tests."""
    to_delete = []

    yield to_delete

    for registered_model in to_delete:
        registered_model.delete()


@pytest.fixture
def model_version(registered_model):
    yield registered_model.get_or_create_version()


@pytest.fixture
def created_endpoints():
    to_delete = []

    yield to_delete

    for endpoint in to_delete:
        endpoint.delete()


@pytest.fixture
def endpoint(client, created_endpoints):
    path = _utils.generate_default_name()
    endpoint = client.create_endpoint(path)
    created_endpoints.append(endpoint)

    yield endpoint


@pytest.fixture
def created_endpoints():
    to_delete = []

    yield to_delete

    for endpoint in to_delete:
        endpoint.delete()


@pytest.fixture
def organization(client):
    workspace_name = _utils.generate_default_name()
    org = client._create_organization(workspace_name)

    yield org

    org.delete()


@pytest.fixture
def requirements_file():
    with tempfile.NamedTemporaryFile('w+') as tempf:
        # create requirements file from pip freeze
        pip_freeze = subprocess.check_output([sys.executable, '-m', 'pip', 'freeze'])
        pip_freeze = six.ensure_str(pip_freeze)
        tempf.write(pip_freeze)
        tempf.flush()  # flush object buffer
        os.fsync(tempf.fileno())  # flush OS buffer
        tempf.seek(0)

        yield tempf
