# -*- coding: utf-8 -*-

import click

from .registry import registry

@registry.group(name="create")
def create():
    """Create a new entry.

    For example, to create a new model with name BERT, run

    `verta registry create model BERT`
    """
    pass

@create.command(name="registeredmodel")
@click.argument("model_name", nargs=1, required=True)
@click.option("--tag", "-t", multiple=True, help="Tags to be associated with the object.")
@click.option("--visibility", "-v", default="private", show_default=True, type=click.Choice(["private", "org"], case_sensitive=False), help="Visibility level of the object.")
@click.option("--workspace", "-w", help="Workspace to use.")
def create_model(model_name, tag, visibility, workspace):
    """Create a new registeredmodel entry.
    """
    pass

@create.command(name="registeredmodelversion")
@click.argument("model_name", nargs=1, required=True)
@click.argument("version_name", nargs=1, required=True)
@click.option("--tag", "-t", multiple=True, help="Tags to be associated with the object.")
@click.option("--model", help="Path to the model.")
@click.option("--asset", multiple=True, help="Path to the asset required for the model. The format if asset_name=path_to_asset.")
@click.option("--workspace", "-w", help="Workspace to use.")
def create_model_version(model_name, tag):
    """Create a new registeredmodelversion entry.
    """
    pass
