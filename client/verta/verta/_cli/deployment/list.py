# -*- coding: utf-8 -*-

import click

from .deployment import deployment
from ... import Client


@deployment.group(name="list")
def lst():
    pass

@lst.command(name="endpoint")
@click.argument("path", nargs=1, required=True)
@click.option("--workspace", "-w", help="Workspace to use.")
def lst_endpoint(path, workspace):
    """List all endpoints available.
    """
    raise NotImplementedError
    # TODO: call client.endpoints, display PATH, ID, and DATE UPDATED
