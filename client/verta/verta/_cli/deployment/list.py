# -*- coding: utf-8 -*-

import click

from .deployment import deployment
from ..AsciiTable import AsciiTable
from ... import Client


@deployment.group(name="list")
def lst():
    """List all instances of an entity related to deployment.

    For example, to list all endpoints, run

    `verta deployment list endpoint`

    """
    pass



@lst.command(name="endpoint")
@click.option("--workspace", "-w", help="Workspace to use.")
def lst_endpoint(workspace):
    """List all endpoints available.
    """
    client = Client()

    endpoints = client.endpoints
    if workspace:
        endpoints = endpoints.with_workspace(workspace)
    table_data = [['PATH', 'ID', 'DATE UPDATED']] + list(sorted(
            map(lambda endpoint: endpoint._get_info_list_by_id(), endpoints), key=lambda data: data[0]))
    table = AsciiTable(table_data)
    click.echo()
    click.echo(table.table)
