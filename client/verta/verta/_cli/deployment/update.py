# -*- coding: utf-8 -*-

import click
import json

from .deployment import deployment
from ... import Client
from ...endpoint.update._strategies import DirectUpdateStrategy, CanaryUpdateStrategy
from ...endpoint.update.rules import _UpdateRule
from ...endpoint.resources import Resources
from ...endpoint.autoscaling import Autoscaling
from ...endpoint.autoscaling.metrics import _AutoscalingMetric
from ...registry.entities import RegisteredModelVersion


@deployment.group()
def update():
    """Update an entity related to deployment.

    For example, to update an endpoint with a new model version, run

    `verta deployment update endpoint "<endpoint path>" -m "<model version id>" -s direct`

    """
    pass

@update.command(name="endpoint")
@click.argument("path", nargs=1, required=True)
@click.option("--autoscaling", help="Quantities for autoscaling. Must also provide --autoscaling-metric.")
@click.option("--autoscaling-metric", multiple=True, help="Metrics for autoscaling. Can only be used alongside --autoscaling.")
@click.option("--canary-interval", "-i", type=click.IntRange(min=0), help="Rollout interval, in seconds. Can only be used alongside --strategy=canary.")
@click.option("--canary-rule", "-c", multiple=True, help="Rule to use for canary deployment. Can only be used alongside --strategy=canary.")
@click.option("--canary-step", type=click.FloatRange(min=0.0, max=1.0), help="Ratio of deployment to roll out per interval. Can only be used alongside --strategy=canary.")
@click.option("--env-vars", type=str, help="Environment variables to set for the model build. The format is --env-vars '{\"VERTA_HOST\": \"app.verta.ai\"}'.")
@click.option("--model-version-id", "-m", help="Model Version to deploy. Cannot be used alongside --run-id.")
@click.option("--filename", "-f", type=click.Path(exists=True, dir_okay=False), help="Path to JSON or YAML config file. Can't be used alongside other options except for --workspace.")
@click.option("--resources", help="Resources allowed for the updated endpoint.")
@click.option("--run-id", "-r", help="Experiment Run to deploy. Cannot be used alongside --model-version-id.")
@click.option("--strategy", "-s", type=click.Choice(['direct', 'canary'], case_sensitive=False), help="Strategy to use to roll out new deployment.")
@click.option("--workspace", "-w", help="Workspace to use.")
# TODO: more options
def update_endpoint(path, run_id, model_version_id, filename, strategy, resources, canary_rule, canary_interval, canary_step, autoscaling, autoscaling_metric, env_vars, workspace):
    """Update an endpoint.
    """
    non_file_options = (run_id, model_version_id, strategy, resources, canary_rule, canary_interval, canary_step, autoscaling, autoscaling_metric, env_vars)

    if filename and any(non_file_options):
        raise click.BadParameter("--filename can't be used alongside other options except for --workspace.")

    if canary_step == 0.0:
        raise click.BadParameter("--canary-step must be positive.")

    if canary_interval == 0:
        raise click.BadParameter("--canary-interval must be positive.")

    canary_options = (canary_rule, canary_interval, canary_step)
    if strategy != "canary" and any(canary_options):
        raise click.BadParameter("--canary-rule, --canary-interval, and --canary-step can only be used alongside --strategy=canary")
    if strategy == "canary" and not all(canary_options):
        raise click.BadParameter("--canary-rule, --canary-interval, and --canary-step must be provided alongside --strategy=canary")

    if autoscaling and not autoscaling_metric:
        raise click.BadParameter("--autoscaling-metric must be provided when using --autoscaling.")

    if autoscaling_metric and not autoscaling:
        raise click.BadParameter("--autoscaling-metric can only be provided when using --autoscaling.")

    client = Client()

    try:
        endpoint = client.get_endpoint(path=path, workspace=workspace)
    except ValueError:
        raise click.BadParameter("endpoint with path {} not found".format(path))

    if filename:
        endpoint.update_from_config(filename)
        return

    if run_id and model_version_id:
        raise click.BadParameter("cannot provide both --run-id and --model-version-id.")
    elif run_id:
        try:
            model_reference = client.get_experiment_run(id=run_id)
        except ValueError:
            raise click.BadParameter("experiment run with id {} not found".format(run_id))
    elif model_version_id:
        try:
            model_reference = RegisteredModelVersion._get_by_id(client._conn, client._conf, model_version_id)
        except ValueError:
            raise click.BadParameter("model version with id {} not found".format(model_version_id))
    else:
        raise click.BadParameter("must provide either --model-version-id or --run-id.")

    if strategy == 'direct':
        strategy_obj = DirectUpdateStrategy()
    else:
        # strategy is canary
        strategy_obj = CanaryUpdateStrategy(canary_interval, canary_step)
        for rule in canary_rule:
            strategy_obj.add_rule(_UpdateRule._from_dict(json.loads(rule)))

    if resources:
        resources = Resources._from_dict(json.loads(resources))
    else:
        resources = None

    if autoscaling:
        autoscaling_obj = Autoscaling._from_dict(json.loads(autoscaling))
        for metric in autoscaling_metric:
            autoscaling_obj.add_metric(_AutoscalingMetric._from_dict(json.loads(metric)))
    else:
        autoscaling_obj = None

    if env_vars:
        env_vars_dict = json.loads(env_vars)
    else:
        env_vars_dict = None

    endpoint.update(model_reference, strategy_obj, resources=resources, autoscaling=autoscaling_obj, env_vars=env_vars_dict)
