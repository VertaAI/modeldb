# -*- coding: utf-8 -*-

import click

from .deployment import deployment
from ... import Client


@deployment.group()
def create():
    """Create an entity related to deployment.

    For example, to create a new endpoint, run

    `verta deployment create endpoint "<endpoint path>"`

    """
    pass

@create.command(name="endpoint")
@click.argument("path", nargs=1, required=True)
@click.option("--workspace", "-w", help="Workspace to use.")
def create_endpoint(path, workspace):
    """Create a new deployment endpoint.
    """
    client = Client()

    client.create_endpoint(path, workspace=workspace)
