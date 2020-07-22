# -*- coding: utf-8 -*-

import click

from .registry import registry
from ... import Client


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
@click.option("--artifact", type=(str, str), multiple=True, help="Path to the artifact required for the model. The format is --artifact artifact_key path_to_artifact.")
@click.option("--workspace", "-w", help="Workspace to use.")
# TODO: add environment
def update_model_version(model_name, version_name, label, model, artifact, workspace):
    """Create a new registeredmodelversion entry.
    """
    client = Client()

    if artifact is not None and len(artifact) > len(set(map(lambda pair: pair[0], artifact))):
        raise click.BadParameter("cannot have duplicate artifact keys")

    try:
        registered_model = client.get_registered_model(model_name, workspace=workspace)
    except ValueError:
        raise click.BadParameter("model {} not found".format(model_name))

    try:
        model_version = registered_model.get_version(name=version_name)
    except ValueError:
        raise click.BadParameter("version {} not found".format(version_name))

    if model_version is None:
        raise click.BadParameter("version {} not found".format(version_name))

    if artifact is not None:
        artifact_keys = model_version.get_artifact_keys()

        for (key, _) in artifact:
            if key == "model":
                raise click.BadParameter("the key \"model\" is reserved for model")

            if key in artifact_keys:
                raise click.BadParameter("key \"{}\" already exists".format(key))

        for (key, path) in artifact:
            model_version.log_artifact(key, path, True)

    if label is not None:
        for l in label:
            model_version.add_label(l)

    if model is not None:
        model_version.log_model(model, True)



