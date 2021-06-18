from __future__ import division

import six
from six.moves import filterfalse

import datetime
import itertools
import os
import pickle
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
from . import constants, utils
from . import clean_test_accounts


RANDOM_SEED = 0
INPUT_LENGTH = 12  # length of iterable input fixture


# hypothesis on Jenkins is apparently too slow
hypothesis.settings.register_profile(
    "default", suppress_health_check=[hypothesis.HealthCheck.too_slow]
)
hypothesis.settings.load_profile("default")


def pytest_addoption(parser):
    parser.addoption("--oss", action="store_true", help="run OSS-compatible tests")


def pytest_collection_modifyitems(config, items):
    if config.getoption("--oss"):
        skip_not_oss = pytest.mark.skip(reason="not available in OSS")
        for item in items:
            if "not_oss" in item.keywords:
                item.add_marker(skip_not_oss)
    else:
        skip_oss = pytest.mark.skip(reason="only applicable to OSS")
        for item in items:
            if "oss" in item.keywords:
                item.add_marker(skip_oss)


@pytest.fixture(autouse=True)
def mark_time():
    print("\n[TEST LOG] test setup begun {} UTC".format(datetime.datetime.utcnow()))
    yield
    print(
        "\n[TEST LOG] test teardown completed {} UTC".format(datetime.datetime.utcnow())
    )


@pytest.fixture(scope="session", autouse=True)
def create_dummy_workspace():
    """Prevent tests from uncontrollably changing accounts' default workspace.

    When an account creates its first organization, or is added to its first
    organization, UAC sets that organization as the account's default
    workspace. This is undesired during test runs, because several tests
    rely on new arbitrary orgs *not* being the active client's default
    workspace.

    This fixture creates a dummy "first" organization for each account, so
    that organizations created for individual tests won't trigger this behavior
    from UAC.

    """
    dummy_orgs = []
    for client in clean_test_accounts.get_clients():
        current_default_workspace = client._conn.get_default_workspace()

        name = _utils.generate_default_name()
        dummy_orgs.append(client._create_organization(name))

        client._conn._set_default_workspace(current_default_workspace)

    yield

    for org in dummy_orgs:
        org.delete()


@pytest.fixture(scope="session")
def host():
    return constants.HOST


@pytest.fixture(scope="session")
def port():
    return constants.PORT


@pytest.fixture(scope="session")
def email():
    return constants.EMAIL


@pytest.fixture(scope="session")
def dev_key():
    return constants.DEV_KEY


# for collaboration tests
@pytest.fixture(scope="session")
def email_2():
    return constants.EMAIL_2


@pytest.fixture(scope="session")
def dev_key_2():
    return constants.DEV_KEY_2


@pytest.fixture(scope="session")
def email_3():
    return constants.EMAIL_3


@pytest.fixture(scope="session")
def dev_key_3():
    return constants.DEV_KEY_3


@pytest.fixture
def seed():
    return RANDOM_SEED


@pytest.fixture
def nones():
    return [None] * INPUT_LENGTH


@pytest.fixture
def bools(seed):
    random.seed(seed)
    return [bool(random.randint(0, 1)) for _ in range(INPUT_LENGTH)]


@pytest.fixture
def floats(seed):
    random.seed(seed)
    return [random.uniform(-(3 ** 2), 3 ** 3) for _ in range(INPUT_LENGTH)]


@pytest.fixture
def ints(seed):
    random.seed(seed)
    return [random.randint(-(3 ** 4), 3 ** 5) for _ in range(INPUT_LENGTH)]


@pytest.fixture
def strs(seed):
    """no duplicates"""
    random.seed(seed)
    gen_str = lambda: "".join(
        random.choice(string.ascii_letters) for _ in range(INPUT_LENGTH)
    )
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
        [values[random.choice(range(len(values)))][i] for i in range(INPUT_LENGTH)]
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
            return [gen_value(p / 2) for _ in range(random.choice(range(4)))]
        else:
            return random.choice(flat_values)

    return [
        [gen_value() for _ in range(random.choice(range(3)) + 1)]
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
                key: gen_value(p / 2)
                for key, _ in zip(strs, range(random.choice(range(4))))
            }
        else:
            return random.choice(flat_values)

    return [
        {key: gen_value() for key, _ in zip(strs, range(random.choice(range(3)) + 1))}
        for _ in range(INPUT_LENGTH)
    ]


@pytest.fixture
def scalar_values(nones, bools, floats, ints, strs):
    return [type_values[0] for type_values in (nones, bools, floats, ints, strs)]


@pytest.fixture
def collection_values(flat_lists, flat_dicts, nested_lists, nested_dicts):
    return [
        type_values[0]
        for type_values in (flat_lists, flat_dicts, nested_lists, nested_dicts)
    ]


@pytest.fixture
def all_values(scalar_values, collection_values):
    return scalar_values + collection_values


@pytest.fixture(scope="session")
def output_path():
    dirpath = ".outputs"
    while len(dirpath) < 1024:
        try:  # avoid name collisions
            os.mkdir(dirpath)
        except OSError:
            dirpath += "_"
        else:
            yield os.path.join(dirpath, "{}")
            break
    else:
        raise RuntimeError("dirpath length exceeded 1024")
    shutil.rmtree(dirpath)


@pytest.fixture(
    params=[  # directory name
        "foo",
        "foo.bar",  # ensure we can handle directories with periods
    ]
)
def dir_and_files(strs, tmp_path, request):
    """
    Creates nested directory of empty files.

    Returns
    -------
    dirpath : str
    filepaths : set of str

    """
    dirpath = tmp_path / request.param

    filepaths = {
        os.path.join(strs[0], strs[1], strs[2]),
        os.path.join(strs[0], strs[1], strs[3]),
        os.path.join(strs[0], strs[2]),
        os.path.join(strs[0], strs[4]),
        os.path.join(strs[2]),
        os.path.join(strs[5]),
    }

    for filepath in filepaths:
        p = dirpath / filepath
        p.parent.mkdir(parents=True, exist_ok=True)
        p.touch()

    return str(dirpath), filepaths


@pytest.fixture
def random_data():
    """
    Returns random bytes that cannot be unpickled,
    which is sometimes the case by chance.

    """
    while True:
        data = os.urandom(2 ** 16)
        bytestream = six.BytesIO(data)
        try:
            pickle.load(bytestream)
        except:
            return data


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
def client(host, port, email, dev_key, created_entities):
    client = Client(host, port, email, dev_key, debug=True)

    yield client

    proj = client._ctx.proj
    if proj is not None and proj.id not in {entity.id for entity in created_entities}:
        proj.delete()


@pytest.fixture(scope="class")
def class_client(host, port, email, dev_key, class_created_entities):
    client = Client(host, port, email, dev_key, debug=True)

    yield client

    proj = client._ctx.proj
    if proj is not None and proj.id not in {
        entity.id for entity in class_created_entities
    }:
        proj.delete()


@pytest.fixture
def client_2(host, port, email_2, dev_key_2, created_entities):
    """For collaboration tests."""
    if not (email_2 and dev_key_2):
        pytest.skip("second account credentials not present")

    client = Client(host, port, email_2, dev_key_2, debug=True)

    return client


@pytest.fixture
def client_3(host, port, email_3, dev_key_3, created_entities):
    """For collaboration tests."""
    if not (email_3 and dev_key_3):
        pytest.skip("second account credentials not present")

    client = Client(host, port, email_3, dev_key_3, debug=True)

    return client


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

    data = pd.DataFrame(
        np.tile(np.arange(num_rows).reshape(-1, 1), num_cols), columns=strs[:num_cols]
    )
    X_train = data.iloc[:, :-1]  # pylint: disable=bad-whitespace
    y_train = data.iloc[:, -1]

    return {
        "model": sklearn.linear_model.LogisticRegression(),
        "model_api": verta.utils.ModelAPI(X_train, y_train),
        "requirements": six.StringIO("scikit-learn=={}".format(sklearn.__version__)),
        "train_features": X_train,
        "train_targets": y_train,
    }


@pytest.fixture
def repository(client, created_entities):
    name = _utils.generate_default_name()
    repo = client.get_or_create_repository(name)
    created_entities.append(repo)

    return repo


@pytest.fixture
def commit(repository):
    commit = repository.get_commit()

    return commit


@pytest.fixture
def registered_model(client, created_entities):
    model = client.get_or_create_registered_model()
    created_entities.append(model)

    return model


@pytest.fixture
def created_entities():
    """Container to track and clean up ModelDB, Registry, etc. entities created during tests."""
    to_delete = []

    yield to_delete

    # move orgs to the end
    from verta.tracking._organization import Organization

    is_org = lambda entity: entity.__class__ is Organization
    to_delete = itertools.chain(
        filterfalse(is_org, to_delete),
        filter(is_org, to_delete),
    )

    # TODO: avoid duplicates
    for entity in to_delete:
        entity.delete()


@pytest.fixture(scope="class")
def class_created_entities():
    """Container to track and clean up ModelDB, Registry, etc. entities created during tests."""
    to_delete = []

    yield to_delete

    # move orgs to the end
    from verta.tracking._organization import Organization

    is_org = lambda entity: entity.__class__ is Organization
    to_delete = itertools.chain(
        filterfalse(is_org, to_delete),
        filter(is_org, to_delete),
    )

    # TODO: avoid duplicates
    for entity in to_delete:
        entity.delete()


@pytest.fixture
def model_version(registered_model):
    yield registered_model.get_or_create_version()


@pytest.fixture
def endpoint(client, created_entities):
    path = _utils.generate_default_name()
    endpoint = client.create_endpoint(path)
    created_entities.append(endpoint)

    return endpoint


@pytest.fixture
def organization(client, created_entities):
    workspace_name = _utils.generate_default_name()
    org = client._create_organization(workspace_name)
    created_entities.append(org)

    return org


@pytest.fixture
def requirements_file():
    with tempfile.NamedTemporaryFile("w+") as tempf:
        # create requirements file from pip freeze
        pip_freeze = subprocess.check_output([sys.executable, "-m", "pip", "freeze"])
        pip_freeze = six.ensure_str(pip_freeze)
        tempf.write(pip_freeze)
        tempf.flush()  # flush object buffer
        os.fsync(tempf.fileno())  # flush OS buffer
        tempf.seek(0)

        yield tempf


@pytest.fixture
def with_boto3():
    """For tests that require AWS's boto3."""
    pytest.importorskip("boto3")
    yield
