# -*- coding: utf-8 -*-

import click

from .registry import registry
from ... import Client


@registry.group(name="archive")
def archive():
    """archive a registry resource

    """
    pass


@archive.command(name="registeredmodelversion")
@click.argument("model_name", nargs=1, required=True)
@click.argument("version_name", nargs=1, required=True)
@click.option("--workspace", "-w", help="Workspace to use")
def archive_registeredmodelversion(model_name, version_name, workspace):
    """Archive registeredmodelversion.
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

    version.archive()
