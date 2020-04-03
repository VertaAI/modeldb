# -*- coding: utf-8 -*-

import click


@click.group()
def environment():
    """Create environment version blobs."""
    pass


@environment.command()
@click.argument('blob')
def python(blob):
    """Create Python environment blob at location BLOB from current environment."""
    click.echo("Python version created at {}".format(blob))
