# -*- coding: utf-8 -*-

import os

import click

# from .._internal_utils import _config_utils

# from . import remote
# from . import branch
# from . import commit
# from . import blob

from .registry.registry import registry
from .deployment.deployment import deployment

CONTEXT_SETTINGS = dict(help_option_names=['-h', '--help'])

@click.group(context_settings=CONTEXT_SETTINGS)
def cli():
    """CLI for the Verta MLOps platform."""
    pass


# @click.command()
# def init():
#     """
#     Create a Verta config file in the current directory.

#     Running verta init in an existing repository is safe. It will not overwrite things that are
#     already there.

#     """
#     for config_filename in _config_utils.CONFIG_FILENAMES:
#         if os.path.isfile(config_filename):
#             config_filepath = os.path.abspath(config_filename)
#             click.echo("found existing config file {}".format(config_filepath))
#             return

#     config_filepath = _config_utils.create_empty_config_file('.')
#     click.echo("initialized empty config file {}".format(config_filepath))


# cli.add_command(init)
cli.add_command(deployment)
cli.add_command(registry)
# cli.add_command(remote.remote)
# cli.add_command(branch.branch)
# cli.add_command(branch.checkout)
# cli.add_command(branch.log)
# cli.add_command(commit.add)
# cli.add_command(commit.rm)
# cli.add_command(commit.commit)
# cli.add_command(commit.tag)
# cli.add_command(commit.status)
# cli.add_command(commit.diff)
# cli.add_command(blob.pull)
# cli.add_command(blob.import_, name="import")
