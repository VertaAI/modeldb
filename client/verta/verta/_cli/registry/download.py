# -*- coding: utf-8 -*-

import click

from ..._internal_utils import _utils
from ... import Client

from .registry import registry


@registry.group(name="download")
def download():
    """Create a new entry.

    For example, to download the Docker context for a model version, run

    `verta registry download dockercontext BERT latest`

    """
    pass


@download.command(name="dockercontext")
@click.argument("model_name", nargs=1, required=True)
@click.argument("version_name", nargs=1, required=True)
@click.option("--output", "-o", required=True, help="Filepath to write to")
def download_docker_context(model_name, version_name, output):
    """Create a new registeredmodelversion entry.
    """
    raise NotImplementedError
