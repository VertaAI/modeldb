# -*- coding: utf-8 -*-

import click


@click.group()
def remote():
    """Configure remote repositories."""
    pass


@remote.command()
@click.argument('name')
@click.argument('url')
def add(name, url):
    """Add a remote repository."""
    click.echo("registered repo {} at url {}".format(name, url))


@remote.command()
@click.argument('name')
def use(name):
    """Set a remote repository as the active one."""
    click.echo("now using repo {}".format(name))
