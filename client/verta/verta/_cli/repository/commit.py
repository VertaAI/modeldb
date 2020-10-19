# -*- coding: utf-8 -*-

import click


@click.command()
@click.argument('blobs', nargs=-1, type=click.Path(exists=True))
def add(blobs):
    """Stage BLOBS to be committed."""
    if not blobs:
        click.echo("no blobs specified, so no blobs added")
        return

    for blob in blobs:
        click.echo("adding {}".format(blob))


@click.command()
@click.argument('blobs', nargs=-1, type=click.Path(exists=True))
def rm(blobs):
    """Unstage BLOBS from being committed."""
    if not blobs:
        click.echo("no blobs specified, so no blobs removed")
        return

    for blob in blobs:
        click.echo("removing {}".format(blob))


@click.command()
@click.option('-a', '--all', is_flag=True, help="Commit all changed blobs.")  # NOTE: this looks difficult
@click.option('-m', '--message', required=True, help="Commit message.")
@click.option('--amend', is_flag=True, help="Amend previous commit.")
def commit(message, all, amend):
    """Record changes to the repository."""
    if all:
        click.echo("adding all blobs")

    if amend:
        click.echo("amending previous commit")

    click.echo("commit saved with message \"{}\"".format(message))


@click.command()
@click.option('-d', '--delete', is_flag=True, help="Delete TAG.")
@click.argument('tag')
def tag(delete, tag):
    """Tag the head commit."""
    if delete:
        msg = "deleting tag {}".format(tag)
    else:
        msg = "tagging head commit as {}".format(tag)
    click.echo(msg)


@click.command()
def status():
    """Show staged changes, unstaged changes, and untracked blobs."""
    click.echo("these blobs have changed")


@click.command()
@click.argument('blob', type=click.Path(exists=True))  # TODO: make optional, to diff all blobs
def diff(blob):
    """Show changes in a blob from the head commit."""
    click.echo("{} has changed in this way".format(blob))
