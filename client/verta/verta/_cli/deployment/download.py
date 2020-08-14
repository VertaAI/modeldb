# -*- coding: utf-8 -*-

import click

from verta import Client
from verta._cli.deployment import deployment


@deployment.group(name="download")
def download():
    """Create a new entry.

    For example, to download the Docker context for a model version, run

    `verta registry download dockercontext BERT latest`

    """
    pass


@download.command(name="dockercontext")
@click.option("--run_id", "-r", help="Experiment Run id")
@click.option("--model_version_id", "-m", help="Model Version id")
@click.option("--output", "-o", required=True, help="Filepath to write to")
def download_docker_context(run_id, model_version_id, output):
    """Download registeredmodelversion's or experiment run's context entry.
    """
    client = Client()

    if run_id:
        try:
            entity = client.get_experiment_run(id=run_id)
        except ValueError:
            raise click.BadParameter("experiment run {} not found".format(run_id))
    elif model_version_id:
        try:
            entity = client.get_registered_model_version(id=model_version_id)
        except ValueError:
            raise click.BadParameter("version {} not found".format(model_version_id))
    else:
        raise click.BadParameter("run-id or model_version_id should be specified")
    entity.download_docker_context(output)
