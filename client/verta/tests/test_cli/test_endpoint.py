from click.testing import CliRunner

from verta import Client
from verta._cli import cli
from verta._internal_utils import _utils

from ..utils import get_build_ids


class TestList:
    def test_list_endpoint(self):
        client = Client()
        path = _utils.generate_default_name()
        path2 = _utils.generate_default_name()
        endpoint1 = client.get_or_create_endpoint(path)
        endpoint2 = client.get_or_create_endpoint(path2)
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'list', 'endpoint'],
        )

        assert not result.exception
        assert path in result.output
        assert path2 in result.output

class TestCreate:
    def test_create_endpoint(self, client, created_endpoints):
        endpoint_name = _utils.generate_default_name()

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'create', 'endpoint', endpoint_name],
        )

        assert not result.exception

        endpoint = client.get_endpoint(endpoint_name)
        assert endpoint

        created_endpoints.append(endpoint)


class TestUpdate:
    def test_direct_update_endpoint(self, client, created_endpoints, experiment_run, model_for_deployment):
        endpoint_name = _utils.generate_default_name()
        endpoint = client.set_endpoint(endpoint_name)
        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "--strategy", "direct"],
        )
        assert not result.exception

        updated_build_ids = get_build_ids(endpoint.get_status())

        assert len(updated_build_ids) - len(updated_build_ids.intersection(original_build_ids)) > 0
        created_endpoints.append(endpoint)

    def test_canary_update_endpoint(self, client, created_endpoints, experiment_run, model_for_deployment):
        endpoint_name = _utils.generate_default_name()
        endpoint = client.set_endpoint(endpoint_name)
        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        canary_rule = '{"rule_id": 1001, "rule_parameters": \
        [{"name": "latency_avg", "value": "0.8"}]}'
        canary_rule_2 = '{"rule_id": 1002, "rule_parameters": \
        [{"name": "error_rate", "value": "0.8"}]}'

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "canary",
             '-c', canary_rule, '-c', canary_rule_2, '-i', 1, "canary-step", 0.3],
        )
        assert not result.exception

        updated_build_ids = get_build_ids(endpoint.get_status())

        assert len(updated_build_ids) - len(updated_build_ids.intersection(original_build_ids)) > 0
        created_endpoints.append(endpoint)

    def test_update_invalid_parameters_error(self, client, created_endpoints, experiment_run):
        error_msg_1 = "--canary-rule, --canary-interval, and --canary-step can only be used alongside --strategy=canary"
        error_msg_2 = "--canary-rule, --canary-interval, and --canary-step must be provided alongside --strategy=canary"

        endpoint_name = _utils.generate_default_name()
        endpoint = client.set_endpoint(endpoint_name)

        canary_rule = '{"rule_id": 1001, "rule_parameters": \
        [{"name": "latency_avg", "value": "0.8"}]}'

        # Extra parameters provided:
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "direct",
             '-i', 1],
        )
        assert result.exception
        assert error_msg_1 in result.output

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "direct",
             '--canary-step', 0.3],
        )
        assert result.exception
        assert error_msg_1 in result.output


        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "direct",
             '-c', canary_rule],
        )
        assert result.exception
        assert error_msg_1 in result.output

        # Missing canary rule:
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "canary",
             '-i', 1, "--canary-step", 0.3],
        )
        assert result.exception
        assert error_msg_2 in result.output

        # Missing interval:
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "canary",
             '-c', canary_rule, "--canary-step", 0.3],
        )
        assert result.exception
        assert error_msg_2 in result.output

        # Missing step:
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "canary",
             '-c', canary_rule, '-i', 1],
        )
        assert result.exception
        assert error_msg_2 in result.output

        created_endpoints.append(endpoint)
