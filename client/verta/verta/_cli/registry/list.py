# -*- coding: utf-8 -*-
import json
from functools import reduce

import click

from .registry import registry
from ... import Client
from ..._internal_utils import _utils


@registry.group(name="list")
def lst():
    """List all objects available.

    For example, to list all registered models, run

    `verta registry list registeredmodel`

    or to get all versions of the model `BERT` available, run

    `verta registry list registeredmodelversion BERT`
    """
    pass

@lst.command(name="registeredmodel")
@click.option("--filter", multiple=True, help="Filters to be applied when listing. See the documentation for a complete list.")
@click.option("--output", "-o", type=click.Choice(['json'], case_sensitive=False), help="Print the output in the given format instead of regular text.")
@click.option("--workspace", "-w", help="Workspace to use")
def lst_model(filter, output, workspace):
    """List all models available.
    """
    client = Client()

    models = client.registered_models.with_workspace(workspace).find(filter)

    click.echo("filter: {}".format(filter))
    click.echo("output: {}".format(output))

    click.echo("result count: {}".format(len(models)))
    click.echo("Listing all models")
    if output == "json":
        array = reduce(lambda a, b: "{}, {}".format(a, b),
               map(lambda model: json.dumps(_utils.proto_to_json(model._msg)), models))
        result = "\"models\": [{}]".format(array)
        result = '{' + result + '}'
        click.echo(result)
    else:
        for model in models:
            model_repr = model._msg.name
            click.echo(model_repr)


@lst.command(name="registeredmodelversion")
@click.argument("model_name", nargs=1, required=False)
@click.option("--filter", multiple=True, help="Filters to be applied when listing. See the documentation for a complete list.")
@click.option("--output", "-o", type=click.Choice(['json'], case_sensitive=False), help="Print the output in the given format instead of regular text.")
@click.option("--workspace", "-w", help="Workspace to use")
def lst_model_version(model_name, filter, output, workspace):
    """List all models available.
    """
    if model_name is None:
        click.echo("Listing versions for all models")
    else:
        click.echo("Listing versions for model {}".format(model_name))
    click.echo("filter: {}".format(filter))
    click.echo("output: {}".format(output))
