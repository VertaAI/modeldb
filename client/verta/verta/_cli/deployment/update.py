# -*- coding: utf-8 -*-

import click

from .deployment import deployment
from ... import Client
from ...deployment.update._strategies import DirectUpdateStrategy, CanaryUpdateStrategy
from ...deployment.update.rules import _UpdateRule


@deployment.group()
def update():
    pass

@update.command(name="endpoint")
@click.argument("path", nargs=1, required=True)
@click.option("--run-id", "-r", help="Experiment Run to deploy.")
@click.option("--strategy", "-s", type=click.Choice(['direct', 'canary'], case_sensitive=False), help="Strategy to use to roll out new deployment.")
@click.option("--canary-rule", "-c", multiple=True, help="Rule to use for canary deployment. Can only be used alongside --strategy=canary")
@click.option("--interval", "-i", type=click.IntRange(min=0), help="Rollout interval, in seconds. Can only be used alongside --strategy=canary")
@click.option("--step", type=click.FloatRange(min=0.0, max=1.0), help="Rollout interval, in seconds. Can only be used alongside --strategy=canary")
@click.option("--workspace", "-w", help="Workspace to use.")
# TODO: more options
def update_endpoint(path, run_id, strategy, canary_rule, interval, step, workspace):
    """List all endpoints available.
    """
    if strategy != "canary" and (canary_rule or interval is not None or step is not None):
        raise click.BadParameter("--canary-rule, --interval, and --step can only be used alongside --strategy=canary")

    if strategy == "canary" and (not canary_rule or interval is None or step is None):
        raise click.BadParameter("--canary-rule, --interval, and --step must be provided alongside --strategy=canary")

    if step is not None and step == 0.0:
        raise click.BadParameter("step must be non-zero.")

    client = Client()

    try:
        endpoint = client.get_endpoint(path=path, workspace=workspace)
    except ValueError:
        raise click.BadParameter("endpoint with path {} not found".format(path))

    try:
        run = client.get_experiment_run(id=run_id)
    except ValueError:
        raise click.BadParameter("experiment run with id {} not found".format(run_id))

    if strategy == 'direct':
        endpoint.update(run, DirectUpdateStrategy())
    else:
        # strategy is canary
        strategy_obj = CanaryUpdateStrategy(interval, step)

        for rule in canary_rule:
            strategy_obj.add_rule(_UpdateRule._from_json(rule))
        endpoint.update(run, strategy_obj)
