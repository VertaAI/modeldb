# -*- coding: utf-8 -*-

import click

from ..._internal_utils import _utils
from ... import Client

from .registry import registry


@registry.group(name="download")
def download():
    """Create a new entry.

    For example, to download the Docker context for a model version, run

    `verta registry download dockercontext BERT latest`

    """
    pass


@download.command(name="dockercontext")
@click.argument("model_name", nargs=1, required=True)
@click.argument("version_name", nargs=1, required=True)
@click.option("--output", "-o", required=True, help="Filepath to write to")
@click.option("--workspace", "-w", help="Workspace to use")
def download_docker_context(model_name, version_name, output, workspace):
    """Download registeredmodelversion's context entry.
    """
    client = Client()

    try:
        model = client.get_registered_model(model_name, workspace=workspace)
    except ValueError:
        raise click.BadParameter("model {} not found".format(model_name))

    try:
        version = model.get_version(name=version_name)
    except ValueError:
        raise click.BadParameter("version {} not found".format(version_name))

    version.download_docker_context(output)
