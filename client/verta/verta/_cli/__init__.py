# -*- coding: utf-8 -*-

import os

import click

# from .._internal_utils import _config_utils

from .registry.registry import registry
from .deployment.deployment import deployment

CONTEXT_SETTINGS = dict(help_option_names=["-h", "--help"])


@click.group(context_settings=CONTEXT_SETTINGS)
def cli():
    """CLI for the Verta MLOps platform."""
    pass


cli.add_command(deployment)
cli.add_command(registry)
