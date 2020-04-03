# -*- coding: utf-8 -*-

import click


@click.command()
@click.option('--repo', help="Repository name")  # TODO: default, get from state
def branch(repo):
    """List available branches"""
    click.echo("You have no branches because this is a demo")
