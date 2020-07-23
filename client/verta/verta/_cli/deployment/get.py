# -*- coding: utf-8 -*-

import click

from .deployment import deployment
from ... import Client


@deployment.group()
def get():
    pass

@get.command(name="endpoint")
@click.argument("path", nargs=1, required=True)
@click.option("--workspace", "-w", help="Workspace to use.")
def get_endpoint(path, workspace):
    """Create detailed information about a deployment endpoint.
    """
    raise NotImplementedError
    # TODO: call client.get_endpoint()
    # TODO: call endpoint.get_status()
