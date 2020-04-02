# -*- coding: utf-8 -*-

import click


@click.group()
def main():
    pass

###### remote ######
@main.group()
def remote():
    """Configure remote repositories"""
    pass

@remote.command()
@click.argument('name')
@click.argument('url')
def add(name, url):
    """Add a remote repository"""
    click.echo("Registered repo {} at url {}".format(name, url))

@remote.command()
@click.argument('name')
def use(name):
    """Set a remote repository as the active one"""
    click.echo("Now using repo {}".format(name))

###### branch ######
@main.command()
@click.option('--repo', help="Repository name")
def branch(repo):
    """List available branches"""
    click.echo("You have no branches because this is a demo")
