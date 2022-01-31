# -*- coding: utf-8 -*-

import pytest

from verta.environment import (
    _Environment,
    Docker,
    Python,
)

from .. import utils


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
