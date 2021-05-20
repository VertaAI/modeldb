# -*- coding: utf-8 -*-

import click

from .deployment import deployment
from ... import Client


@deployment.group()
def get():
    """Get detailed information about an entity related to deployment.

    For example, to see information about an endpoint, run

    `verta deployment get endpoint "<endpoint path>"`

    """
    pass

@get.command(name="endpoint")
@click.argument("path", nargs=1, required=True)
@click.option("--workspace", "-w", help="Workspace to use.")
def get_endpoint(path, workspace):
    """Get detailed information about a deployment endpoint.
    """
    client = Client()

    try:
        endpoint = client.get_endpoint(path, workspace=workspace)
    except ValueError:
        raise click.BadParameter("endpoint {} not found".format(path))

    click.echo()
    click.echo(endpoint)
