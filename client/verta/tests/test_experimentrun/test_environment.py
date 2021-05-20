import pytest

from verta.environment import Python


class TestLogEnvironment:

    def test_environment_type_error(self, experiment_run):
        with pytest.raises(TypeError):
            experiment_run.log_environment('Not a `Python` object')

    def test_log_good_environment(self, experiment_run):
        assert not experiment_run.has_environment

        python_env = Python([])
        experiment_run.log_environment(python_env)
        assert experiment_run.has_environment

        stored_python_env = experiment_run.get_environment()
        local_python_version = python_env._msg.python.version
        stored_python_version = stored_python_env._msg.python.version

        assert local_python_version.major == stored_python_version.major
        assert local_python_version.minor == stored_python_version.minor
        assert local_python_version.patch == stored_python_version.patch

    def test_overwrite_existing(self, experiment_run):
        assert not experiment_run.has_environment

        empty_python_env = Python([])
        experiment_run.log_environment(empty_python_env)
        assert experiment_run.has_environment
        reqs_before = _extract_requirements(experiment_run.get_environment())

        python_env = Python(['tensorflow==1.2.3'])
        experiment_run.log_environment(python_env, overwrite=True)
        assert experiment_run.has_environment
        reqs_after = _extract_requirements(experiment_run.get_environment())
        assert len(reqs_after) == (len(reqs_before) + 1)

    def test_no_overwrite(self, experiment_run):
        python_env = Python([])
        experiment_run.log_environment(python_env)
        assert experiment_run.has_environment
        with pytest.raises(ValueError):
            experiment_run.log_environment(python_env, overwrite=False)


def _extract_requirements(python_env):
    return python_env._msg.python.requirements
