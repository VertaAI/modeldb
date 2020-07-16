# -*- coding: utf-8 -*-

import click


@click.group()
def code():
    """Create code version blobs."""
    pass


@code.command()
@click.argument('blob')
def git(blob):
    """Create Git code blob at location BLOB from current environment."""
    click.echo("git version created at {}".format(blob))
