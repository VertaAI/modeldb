# -*- coding: utf-8 -*-

import click

from . import remote
from . import branch
from . import commit
from . import blob


@click.group()
def cli():
    """ModelDB versioning CLI for snapshotting and tracking model ingredients."""
    pass


cli.add_command(remote.remote)
cli.add_command(branch.branch)
cli.add_command(branch.checkout)
cli.add_command(branch.log)
cli.add_command(commit.add)
cli.add_command(commit.rm)
cli.add_command(commit.commit)
cli.add_command(commit.tag)
cli.add_command(commit.status)
cli.add_command(commit.diff)
cli.add_command(blob.pull)
cli.add_command(blob.import_, name="import")
