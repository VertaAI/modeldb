# -*- coding: utf-8 -*-

import pytest

from verta.tracking.entities._deployable_entity import _DeployableEntity
from verta.tracking.entities import ExperimentRun
from verta.registry.entities import RegisteredModelVersion
from verta.environment import (
    _Environment,
    Docker,
    Python,
)

from .. import utils


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


@pytest.fixture(params=utils.sorted_subclasses(_Environment))
def environment(request):
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
