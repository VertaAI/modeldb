# -*- coding: utf-8 -*-

import click

from .deployment import deployment
from ... import Client
from terminaltables import AsciiTable


@deployment.group(name="list")
def lst():
    pass



@lst.command(name="endpoint")
@click.option("--workspace", "-w", help="Workspace to use.")
def lst_endpoint(workspace):
    """List all endpoints available.
    """
    client = Client()

    endpoints = client.endpoints(workspace)
    table_data = [['PATH', 'ID', 'DATE UPDATED']] + list(map(lambda endpoint: endpoint._get_info_list_by_id(), endpoints))
    table = AsciiTable(table_data)
    print(table.table)
    # TODO: call client.endpoints, display PATH, ID, and DATE UPDATED
