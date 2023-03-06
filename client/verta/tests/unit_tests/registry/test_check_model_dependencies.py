# -*- coding: utf-8 -*-
""" Unit tests for verta.registry.check_model_dependencies """

import pytest
from typing import Set
import warnings

from verta.registry.check_model_dependencies import _check_model_dependencies
from verta.environment import Python


# TODO VRD-682 convert module names to pkg names and update this
@pytest.fixture(scope='session')
def complete_env() -> Python:
    """ Environment with all expected packages from dependency_testing_model
    fixture.
    """
    return Python([
        'calendar==0.0.1',
        'click==0.0.1',
        'cloudpickle==0.0.1',
        'collections==0.0.1',
        'datetime==0.0.1',
        'google==0.0.1',
        'json==0.0.1',
        'numpy==0.0.1',
        'pandas==0.0.1',
        'PIL==0.0.1',
        'requests==0.0.1',
        'requests==0.0.1',
        'sklearn==0.0.1',
        'torch==0.0.1',
        'urllib3==0.0.1',
        'verta==0.0.1',
        'yaml==0.0.1',
    ])


def test_check_model_dependencies_complete(dependency_testing_model, complete_env) -> None:
    """ Verify that check_model_dependencies returns all the expected packages from
    the test model class (dependency_testing_model fixture).
    """
    assert _check_model_dependencies(
        model_cls=dependency_testing_model,
        environment=complete_env,
        raise_for_missing=False,
    )


def test_check_model_dependencies_missing_raise(dependency_testing_model, complete_env) -> None:
    """ Verify that check_model_dependencies raises an exception for missing
    packages when `raise_for_missing`` is True.
    """
    incomplete_env = Python(
        [ r for r in complete_env.requirements if r != 'click==0.0.1' ]
    )  # drop a single dependency to be caught
    with pytest.raises(RuntimeError) as err:
        _check_model_dependencies(
            model_cls=dependency_testing_model,
            environment=incomplete_env,
            raise_for_missing=True,
        )
    assert err.value.args[0] == "the following packages are required by the model " \
                                "but missing from the environment: {'click'}"


def test_check_model_dependencies_missing_warning(dependency_testing_model, complete_env) -> None:
    """ Verify that check_model_dependencies does not raise an exception for
    missing packages when `raise_for_missing`` is False.
    """
    incomplete_env = Python(
        [ r for r in complete_env.requirements if r != 'pandas==0.0.1' ]
    )  # drop a single dependency to be caught
    with warnings.catch_warnings(record=True) as caught_warnings:
        assert not _check_model_dependencies(
            model_cls=dependency_testing_model,
            environment=incomplete_env,
            raise_for_missing=False,
        )
    warn_msg = caught_warnings[0].message.args[0]
    assert warn_msg == "the following packages are required by the model but " \
                       "missing from the environment: {'pandas'}"
