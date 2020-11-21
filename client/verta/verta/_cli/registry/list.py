# -*- coding: utf-8 -*-
import json
from functools import reduce

import click

from .registry import registry
from ..AsciiTable import AsciiTable
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

    models = client.registered_models.with_workspace(workspace).find(*filter)

    click.echo()
    if output == "json":
        array = reduce(lambda a, b: "{}, {}".format(a, b),
               map(lambda model: json.dumps(_utils.proto_to_json(model._msg)), models))
        result = "\"models\": [{}]".format(array)
        result = '{' + result + '}'
        click.echo(result)
    else:
        table_data = [['MODEL NAME', 'ID', 'DATE UPDATED']] + list(sorted(
            map(lambda model: model._get_info_list(), models), key=lambda data: data[0]))
        table = AsciiTable(table_data)
        click.echo(table.table)


@lst.command(name="registeredmodelversion")
@click.argument("model_name", nargs=1, required=False)
@click.option("--filter", multiple=True, help="Filters to be applied when listing. See the documentation for a complete list.")
@click.option("--output", "-o", type=click.Choice(['json'], case_sensitive=False), help="Print the output in the given format instead of regular text.")
@click.option("--workspace", "-w", help="Workspace to use")
def lst_model_version(model_name, filter, output, workspace):
    """List all models available.
    """
    client = Client()

    if model_name is None:
        click.echo("Listing versions for all models")
        registered_model = None
    else:
        click.echo("Listing versions for model {}".format(model_name))
        registered_model = client.get_registered_model(model_name, workspace)

    model_versions = client.registered_model_versions.with_model(registered_model).find(*filter)

    click.echo()
    if output == "json":
        array = reduce(lambda a, b: "{}, {}".format(a, b),
                       map(lambda model_version: json.dumps(_utils.proto_to_json(model_version._msg)), model_versions))
        result = "\"model versions\": [{}]".format(array)
        result = '{' + result + '}'
        click.echo(result)
    else:
        if model_name is None:
            id_or_name = 'MODEL ID'
        else :
            id_or_name = 'MODEL NAME'
        table_data = [['MODEL VERSION NAME', 'ID', id_or_name, 'DATE UPDATED']] + list(sorted(
            map(lambda model_version: model_version._get_info_list(model_name), model_versions), key=lambda data: data[0]))
        table = AsciiTable(table_data)
        click.echo(table.table)
