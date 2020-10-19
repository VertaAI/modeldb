# -*- coding: utf-8 -*-

import json

import click

from ..._internal_utils import _utils
from ... import Client

from .registry import registry


@registry.group(name="get")
def get():
    """Get detailed information about an object.

    For example, to see information about a registered model named BERT, run

    `verta registry get registeredmodel BERT`

    or to get the BERT model with the version latest, run

    `verta registry get registeredmodelversion BERT latest`

    """
    pass


@get.command(name="registeredmodel")
@click.argument("model_name", nargs=1, required=True)
@click.option("--output", "-o", type=click.Choice(['json'], case_sensitive=False), help="Print the output in the given format instead of regular text.")
@click.option("--workspace", "-w", help="Workspace to use")
def get_model(model_name, output, workspace):
    """Get detailed information about a model.
    """
    client = Client()

    try:
        model = client.get_registered_model(model_name, workspace=workspace)
    except ValueError:
        raise click.BadParameter("model {} not found".format(model_name))

    if output == "json":
        model_repr = json.dumps(_utils.proto_to_json(model._msg))
    else:
        model_repr = repr(model)

    click.echo()
    click.echo(model_repr)

@get.command(name="registeredmodelversion")
@click.argument("model_name", nargs=1, required=True)
@click.argument("version_name", nargs=1, required=True)
@click.option("--output", "-o", type=click.Choice(['json'], case_sensitive=False), help="Print the output in the given format instead of regular text.")
@click.option("--workspace", "-w", help="Workspace to use")
def get_model_version(model_name, version_name, output, workspace):
    """Get detailed information about a model version.
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

    if output == "json":
        version_repr = json.dumps(_utils.proto_to_json(version._msg))
    else:
        version_repr = repr(version)

    click.echo()
    click.echo(version_repr)
