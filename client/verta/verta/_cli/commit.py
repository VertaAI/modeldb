# -*- coding: utf-8 -*-

import click


@click.command()
@click.argument('blobs', nargs=-1, type=click.Path(exists=True))
def add(blobs):
    """Stage BLOBS to be committed."""
    if not blobs:
        pass
    pass


@click.command()
@click.argument('blobs', nargs=-1, type=click.Path(exists=True))
def rm(blobs):
    """Unstage BLOBS from being committed."""
    if not blobs:
        pass
    pass


@click.command()
@click.option('-m', '--message', help="Commit message.")
@click.option('-a', '--all', is_flag=True, help="Commit all changed blobs.")  # NOTE: this looks difficult
def commit(message, all):
    """Record changes to the repository."""
    if all:
        pass
    pass


@click.command()
def status():
    """Show staged changes, unstaged changes, and untracked blobs."""
    pass


@click.command()
@click.argument('blob', type=click.Path(exists=True))  # TODO: make optional, to diff all blobs
def diff(blob):
    """Show changes in a blob from the current commit."""
    pass
