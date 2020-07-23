# -*- coding: utf-8 -*-

import click

from .deployment import deployment
from ... import Client


@deployment.group()
def update():
    pass

@update.command(name="endpoint")
@click.argument("path", nargs=1, required=True)
@click.option("--run-id", "-r", help="Experiment Run to deploy.")
@click.option("--strategy", "-s", type=click.Choice(['direct', 'canary'], case_sensitive=False), help="Strategy to use to roll out new deployment.")
@click.option("--canary-rule", "-c", multiple=True, help="Rule to use for canary deployment. Can only be used alongside --strategy=canary")
@click.option("--workspace", "-w", help="Workspace to use.")
# TODO: more options
def update_endpoint(path, run_id, strategy, canary_rule, workspace):
    """List all endpoints available.
    """
    raise NotImplementedError
