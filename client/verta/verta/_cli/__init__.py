# -*- coding: utf-8 -*-

import click

from . import remote
from . import branch


@click.group()
def cli():
    pass


cli.add_command(remote.remote)
cli.add_command(branch.branch)
cli.add_command(branch.checkout)
