# -*- coding: utf-8 -*-

import click

from .blobs import code, dataset, environment


@click.command()
# TODO: figure out how to make arguments optional
@click.argument('repo')
@click.argument('branch')
def pull(repo, branch):
    """Pull all blobs from REPO BRANCH, mirroring their remote locations."""
    click.echo("pulling blobs from {}'s {} branch".format(repo, branch))


@click.group()
def import_():
    """Create versioning blobs."""
    pass


import_.add_command(code.code)
import_.add_command(dataset.dataset)
import_.add_command(environment.environment)
