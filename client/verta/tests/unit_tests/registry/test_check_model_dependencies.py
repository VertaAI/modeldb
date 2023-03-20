# -*- coding: utf-8 -*-
""" Unit tests for verta.registry.check_model_dependencies """

import warnings

import pytest

from verta.environment import Python
from verta.registry import check_model_dependencies


@pytest.fixture(scope='session')
def complete_env() -> Python:
    """ Environment with all 3rd-party packages expected to be extracted
    from the dependency_testing_model fixture.
    """
    return Python([
        'click==0.0.1',
        'googleapis-common-protos==0.0.1',
        'numpy==0.0.1',
        'pandas==0.0.1',
        'Pillow==0.0.1',
        'requests==0.0.1',
        'scikit-learn==0.0.1',
        'torch==0.0.1',
        'urllib3==0.0.1',
        'PyYAML==0.0.1',
    ])  # `verta` and `cloudpickle` included by default


def test_check_model_dependencies_complete(dependency_testing_model, complete_env) -> None:
    """ Verify that check_model_dependencies extracts all the expected packages from
    the test model class (dependency_testing_model fixture) and correctly reconciles
    them against the provided environment (complete_env fixture).
    """
    assert check_model_dependencies(
        model_cls=dependency_testing_model,
        environment=complete_env,
        raise_for_missing=False,
    )


def test_check_model_dependencies_missing_raise(dependency_testing_model, complete_env) -> None:
    """ Verify that check_model_dependencies raises an exception, with the
    correct message, for missing packages when `raise_for_missing` is True.
    """
    incomplete_env = Python(
        [r for r in complete_env.requirements if r != 'click==0.0.1']
    )  # drop a single dependency to be caught
    with pytest.raises(RuntimeError) as err:
        check_model_dependencies(
            model_cls=dependency_testing_model,
            environment=incomplete_env,
            raise_for_missing=True,
        )
    assert err.value.args[0] == "the following packages are required by the model but missing " \
                                "from the environment:\nclick (installed via ['click'])"


def test_check_model_dependencies_missing_warning(dependency_testing_model, complete_env) -> None:
    """ Verify that check_model_dependencies defaults to raising a warning, with
    the correct message, for missing packages when `raise_for_missing` is False.
    """
    incomplete_env = Python(
        [r for r in complete_env.requirements if r not in ['PyYAML==0.0.1', 'pandas==0.0.1']]
    )  # drop a single dependency to be caught
    with warnings.catch_warnings(record=True) as caught_warnings:
        assert not check_model_dependencies(
            model_cls=dependency_testing_model,
            environment=incomplete_env,
            raise_for_missing=False,
        )
    warn_msg = caught_warnings[0].message.args[0]
    assert warn_msg == "the following packages are required by the model but missing " \
                       "from the environment:\npandas (installed via ['pandas'])" \
                       "\nyaml (installed via ['PyYAML'])"
