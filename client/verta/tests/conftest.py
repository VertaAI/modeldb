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

import filelock
import requests

import verta
from verta import Client
from verta._internal_utils import _utils, _pip_requirements_utils
from verta.environment import _Environment, Docker, Python
from verta.tracking.entities._deployable_entity import _DeployableEntity
from verta.tracking.entities import ExperimentRun
from verta.registry.entities import RegisteredModelVersion

import hypothesis
import pytest

pytest.register_assert_rewrite("tests.utils")
from . import constants, utils
from . import clean_test_accounts
from .env_fixtures import (
    mock_env_dev_key_auth,
    mock_env_jwt_auth,
    mock_env_authn_missing,
)
from verta._internal_utils._utils import generate_default_name
from verta._protos.public.uac import RoleV2_pb2


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
def email_sys_admin():
    return constants.EMAIL_SYS_ADMIN


@pytest.fixture(scope="session")
def dev_key_sys_admin():
    return constants.DEV_KEY_SYS_ADMIN


@pytest.fixture(scope="session")
def namespace():
    """For use with a workspace in multiple-namespace setups."""
    return constants.NAMESPACE_MNS


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
    return [random.uniform(-(3**2), 3**3) for _ in range(INPUT_LENGTH)]


@pytest.fixture
def ints(seed):
    random.seed(seed)
    return [random.randint(-(3**4), 3**5) for _ in range(INPUT_LENGTH)]


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
        data = os.urandom(2**16)
        bytestream = six.BytesIO(data)
        try:
            pickle.load(bytestream)
        except:
            return data


@pytest.fixture
def in_tempdir():
    """Moves test to execute inside a temporary directory."""
    with utils.chtempdir() as dirpath:
        yield dirpath


@pytest.fixture
def client(host, port, email, dev_key, created_entities):
    client = Client(host, port, email, dev_key, debug=True)

    yield client

    proj = client._ctx.proj
    if proj is not None and proj.id not in {entity.id for entity in created_entities}:
        proj.delete()


@pytest.fixture
def https_client(host, email, dev_key, created_entities):
    """A Client that is guaranteed to be using HTTPS for its connection.

    Our test suite uses HTTP by default to make faster intra-cluster requests.

    """
    https_verta_url = os.environ.get(constants.HTTPS_VERTA_URL_ENV_VAR)
    if not https_verta_url and ".verta.ai" in host and not host.startswith("http://"):
        https_verta_url = host
    if not https_verta_url:
        pytest.skip("no HTTPS Verta URL available")

    client = Client(https_verta_url, email=email, dev_key=dev_key, debug=True)

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
def client_sys_admin(host, port, email_sys_admin, dev_key_sys_admin, created_entities):
    """For collaboration tests."""
    if not (email_sys_admin and dev_key_sys_admin):
        pytest.skip("sys admin account credentials not present")

    client = Client(host, port, email_sys_admin, dev_key_sys_admin, debug=True)

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
def dataset(client, created_entities):
    dataset = client.create_dataset()
    created_entities.append(dataset)

    return dataset


def registered_model_factory(client_param, created_entities_param):
    model = client_param.get_or_create_registered_model()
    created_entities_param.append(model)
    return model


@pytest.fixture
def registered_model(client, created_entities):
    return registered_model_factory(client, created_entities)


@pytest.fixture(scope="class")
def class_registered_model(class_client, class_created_entities):
    return registered_model_factory(class_client, class_created_entities)


@pytest.fixture(params=utils.sorted_subclasses(_DeployableEntity))
def deployable_entity(request, client, created_entities):
    cls = request.param
    if cls is ExperimentRun:
        proj = client.create_project()
        created_entities.append(proj)
        entity = client.create_experiment_run()
    elif cls is RegisteredModelVersion:
        reg_model = client.create_registered_model()
        created_entities.append(reg_model)
        entity = reg_model.create_version()
    else:
        raise RuntimeError(
            "_DeployableEntity appears to have a subclass {} that is not"
            " accounted for in this fixture".format(cls)
        )

    return entity


@pytest.fixture
def created_entities():
    """Container to track and clean up ModelDB, Registry, etc. entities created during tests."""
    to_delete = []

    yield to_delete

    # move workspaces to the end
    from verta._uac._workspace import Workspace

    is_workspace = lambda entity: entity.__class__ is Workspace
    to_delete = itertools.chain(
        filterfalse(is_workspace, to_delete),
        filter(is_workspace, to_delete),
    )

    # TODO: avoid duplicates
    for entity in to_delete:
        entity.delete()


@pytest.fixture(scope="class")
def class_created_entities():
    """Container to track and clean up ModelDB, Registry, etc. entities created during tests."""
    to_delete = []

    yield to_delete

    # TODO: avoid duplicates
    for entity in to_delete:
        entity.delete()


@pytest.fixture
def model_version(registered_model):
    yield registered_model.get_or_create_version()


def endpoint_factory(client_param, created_entities_param):
    path = _utils.generate_default_name()
    endpoint = client_param.create_endpoint(path)
    created_entities_param.append(endpoint)
    return endpoint


@pytest.fixture
def endpoint(client, created_entities):
    return endpoint_factory(client, created_entities)


@pytest.fixture(scope="class")
def class_endpoint_updated(
    class_client, class_registered_model, class_created_entities
):
    ep = endpoint_factory(class_client, class_created_entities)
    mv = class_registered_model.get_or_create_version()

    class EchoModel(object):
        def predict(self, x):
            return x

    mv.log_model(EchoModel())
    mv.log_environment(Python(requirements=[]))
    ep.update(mv)
    return ep


@pytest.fixture
def workspace(client_sys_admin, created_entities):
    return create_workspace(
        client_sys_admin,
        created_entities,
        [
            RoleV2_pb2.RoleResourceActions(
                resource_type=RoleV2_pb2.ResourceTypeV2.ENDPOINT,
                allowed_actions=[RoleV2_pb2.ActionTypeV2.READ],
            ),
            RoleV2_pb2.RoleResourceActions(
                resource_type=RoleV2_pb2.ResourceTypeV2.REGISTERED_MODEL,
                allowed_actions=[RoleV2_pb2.ActionTypeV2.READ,
                                 RoleV2_pb2.ActionTypeV2.UPDATE],
            ),
        ],
    )


@pytest.fixture
def workspace2(client_sys_admin, created_entities):
    return create_workspace(client_sys_admin, created_entities, [
    RoleV2_pb2.RoleResourceActions(resource_type=RoleV2_pb2.ResourceTypeV2.ENDPOINT,
                                   allowed_actions=[RoleV2_pb2.ActionTypeV2.READ]),
    RoleV2_pb2.RoleResourceActions(resource_type=RoleV2_pb2.ResourceTypeV2.REGISTERED_MODEL,
                                   allowed_actions=[RoleV2_pb2.ActionTypeV2.CREATE, RoleV2_pb2.ActionTypeV2.READ,
                                                    RoleV2_pb2.ActionTypeV2.UPDATE, RoleV2_pb2.ActionTypeV2.DELETE]),
])


@pytest.fixture
def workspace3(client_sys_admin, created_entities):
    return create_workspace(client_sys_admin, created_entities, [
    RoleV2_pb2.RoleResourceActions(resource_type=RoleV2_pb2.ResourceTypeV2.ENDPOINT,
                                   allowed_actions=[RoleV2_pb2.ActionTypeV2.READ]),
    RoleV2_pb2.RoleResourceActions(resource_type=RoleV2_pb2.ResourceTypeV2.REGISTERED_MODEL,
                                   allowed_actions=[RoleV2_pb2.ActionTypeV2.READ,
                                                    RoleV2_pb2.ActionTypeV2.UPDATE]),
])


@pytest.fixture
def workspace_mns(client_sys_admin, created_entities, namespace_mns):
    return create_workspace(client_sys_admin, created_entities, [
    RoleV2_pb2.RoleResourceActions(resource_type=RoleV2_pb2.ResourceTypeV2.ENDPOINT,
                                   allowed_actions=[RoleV2_pb2.ActionTypeV2.READ]),
    RoleV2_pb2.RoleResourceActions(resource_type=RoleV2_pb2.ResourceTypeV2.REGISTERED_MODEL,
                                   allowed_actions=[RoleV2_pb2.ActionTypeV2.READ,
                                                    RoleV2_pb2.ActionTypeV2.UPDATE]),
], namespace=namespace_mns)


def create_workspace(client, created_entities, roles, namespace=""):
    workspace = client._create_workspace(client._conn._get_organization_id(), generate_default_name(), roles, namespace)
    created_entities.append(workspace)
    return workspace


@pytest.fixture
def requirements_file():
    """Create requirements file from pip freeze."""
    pip_freeze = _pip_requirements_utils.get_pip_freeze()
    pip_freeze = _pip_requirements_utils.clean_reqs_file_lines(pip_freeze)

    with tempfile.NamedTemporaryFile("w+") as tempf:
        tempf.write("\n".join(pip_freeze))
        tempf.flush()  # flush object buffer
        os.fsync(tempf.fileno())  # flush OS buffer
        tempf.seek(0)

        yield tempf


@pytest.fixture
def with_boto3():
    """For tests that require AWS's boto3."""
    pytest.importorskip("boto3")
    yield


@pytest.fixture(params=utils.sorted_subclasses(_Environment))
def environment(request):
    # TODO: move to deployable_entity/conftest.py when no longer used in registry/model_version/test_deployment.py
    cls = request.param
    if cls is Docker:
        env = Docker(
            repository="012345678901.dkr.ecr.apne2-az1.amazonaws.com/models/example",
            tag="example",
        )
    elif cls is Python:
        env = Python(requirements=["pytest=={}".format(pytest.__version__)])
    else:
        raise RuntimeError(
            "_Environment appears to have a subclass {} that is not"
            " accounted for in this fixture".format(cls)
        )

    return env
