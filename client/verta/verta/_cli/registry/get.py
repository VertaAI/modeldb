# -*- coding: utf-8 -*-

import click

from .registry import registry

@registry.group(name="get")
def get():
    """Get detailed information about an object.

    For example, to see information about a registered model named BERT, run

    `verta registry get registeredmodel BERT`

    or to get the BERT model with the version latest, run

    `verta registry get registeredmodelversion BERT latest`

    """
    pass


@get.command(name="registeredmodel")
@click.argument("model_name", nargs=1, required=True)
@click.option("--output", "-o", type=click.Choice(['json'], case_sensitive=False), help="Print the output in the given format instead of regular text.")
@click.option("--workspace", "-w", help="Workspace to use")
def get_model(model_name, output, workspace):
    """Get detailed information about a model.
    """
    pass

@get.command(name="registeredmodelversion")
@click.argument("model_name", nargs=1, required=True)
@click.argument("version_name", nargs=1, required=True)
@click.option("--output", "-o", type=click.Choice(['json'], case_sensitive=False), help="Print the output in the given format instead of regular text.")
@click.option("--workspace", "-w", help="Workspace to use")
def get_model_version(model_name, version_name, output, workspace):
    """Get detailed information about a model version.
    """
    pass
