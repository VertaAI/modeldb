# -*- coding: utf-8 -*-

import click

from .registry import registry

@registry.group(name="update")
def update():
    """Update an existing entry.
    """
    pass

@update.command(name="registeredmodel")
@click.argument("model_name", nargs=1, required=True)
@click.option("--label", "-l", multiple=True, help="Label to be associated with the object.")
@click.option("--workspace", "-w", help="Workspace to use.")
def update_model(model_name, label, workspace):
    """Create a new registeredmodel entry.
    """
    pass

@update.command(name="registeredmodelversion")
@click.argument("model_name", nargs=1, required=True)
@click.argument("version_name", nargs=1, required=True)
@click.option("--label", "-l", multiple=True, help="Label to be associated with the object.")
@click.option("--model", help="Path to the model.")
@click.option("--asset", multiple=True, help="Path to the asset required for the model. The format if asset_name=path_to_asset.")
@click.option("--workspace", "-w", help="Workspace to use.")
# TODO: add environment
def update_model_version(model_name, version_name, label, model, asset, workspace):
    """Create a new registeredmodelversion entry.
    """
    pass
