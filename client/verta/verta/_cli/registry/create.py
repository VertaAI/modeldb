# -*- coding: utf-8 -*-

import click

from .registry import registry
from ... import Client


@registry.group(name="create")
def create():
    """Create a new entry.

    For example, to create a new model with name BERT, run

    `verta registry create registeredmodel BERT`

    or to create a BERT model with the version latest, run

    `verta registry create registeredmodelversion BERT latest`

    """
    pass

@create.command(name="registeredmodel")
@click.argument("model_name", nargs=1, required=True)
@click.option("--label", "-l", multiple=True, help="Labels to be associated with the object.")
@click.option("--visibility", "-v", default="private", show_default=True, type=click.Choice(["private", "org"], case_sensitive=False), help="Visibility level of the object.")
@click.option("--workspace", "-w", help="Workspace to use.")
def create_model(model_name, label, visibility, workspace):
    """Create a new registeredmodel entry.
    """
    public_within_org = visibility == "org"

@create.command(name="registeredmodelversion")
@click.argument("model_name", nargs=1, required=True)
@click.argument("version_name", nargs=1, required=True)
@click.option("--label", "-l", multiple=True, help="Labels to be associated with the object.")
@click.option("--model", help="Path to the model.")
@click.option("--artifact", type=(str, str), multiple=True, help="Path to the artifact required for the model. The format is --artifact artifact_key path_to_artifact.")
@click.option("--workspace", "-w", help="Workspace to use.")
def create_model_version(model_name, version_name, label, model, artifact, workspace):
    """Create a new registeredmodelversion entry.
    """
    client = Client()

    try:
        registered_model = client.get_registered_model(name=model_name, workspace=workspace)
    except ValueError:
        raise click.BadParameter("model {} not found".format(model_name))

    model_version = registered_model.get_or_create_version(name=version_name, labels=list(label))

    if model is not None:
        model_version.log_model(model, True)

    if artifact is not None:
        for (key, _) in artifact:
            if key == "model":
                raise click.BadParameter("the key \"model\" is reserved for model")

        for (key, path) in artifact:
            model_version.log_artifact(key, path, True)

