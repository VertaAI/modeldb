# -*- coding: utf-8 -*-

import click

from .deployment import deployment
from ... import Client


@deployment.group()
def create():
    pass

@create.command(name="endpoint")
@click.argument("path", nargs=1, required=True)
@click.option("--workspace", "-w", help="Workspace to use.")
def create_endpoint(path, workspace):
    """Create a new deployment endpoint.
    """
    client = Client()

    client.get_or_create_endpoint(path, workspace=workspace)
    # TODO: call client.get_or_create_endpoint()
