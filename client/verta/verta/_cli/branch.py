# -*- coding: utf-8 -*-

import click


@click.command()
@click.option('--repo', help="Repository name.")  # TODO: default, get from state
def branch(repo):
    """List available branches."""
    click.echo("you have no branches because this is a demo")


@click.command()
@click.option('-b', is_flag=True, help="Create and checkout a new branch.")
@click.argument('branch')
@click.option('--repo', help="Repository name")  # TODO: default, get from state
def checkout(b, branch, repo):
    """Checkout a branch to work on."""
    msg = "checking out {} from repo {}".format(branch, repo)
    if b:
        msg = "creating and "+msg
    click.echo(msg)


@click.command()
def log():
    """Show commit logs."""
    click.echo("here's the log")
