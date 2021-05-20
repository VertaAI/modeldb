# -*- coding: utf-8 -*-

import click

from .registry import registry
from ... import Client
from .update import update_model_version


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
@click.option("--description", "-d", help="Description.")
@click.option("--label", "-l", multiple=True, help="Labels to be associated with the object.")
@click.option("--visibility", "-v", default="private", show_default=True, type=click.Choice(["private", "org"], case_sensitive=False), help="Visibility level of the object.")
@click.option("--workspace", "-w", help="Workspace to use.")
def create_model(model_name, label, visibility, workspace, description):
    """Create a new registeredmodel entry.
    """
    public_within_org = visibility == "org"
    client = Client()

    model = client.create_registered_model(model_name, workspace=workspace, public_within_org=public_within_org,
                                           desc=description)
    for l in label:
        model.add_label(l)

@create.command(name="registeredmodelversion")
@click.argument("model_name", nargs=1, required=True)
@click.argument("version_name", nargs=1, required=True)
@click.option("--artifact", type=str, multiple=True, help="Path to an artifact required for the model. The format is --artifact artifact_key=path_to_artifact.")
@click.option("--attribute", type=str, multiple=True, help="Attribute to be associated with the model version. The format is --attribute attribute_key=value.")
@click.option("--custom-module", type=click.Path(exists=True), multiple=True, help="Path to custom module file or directory.")
@click.option("--from-run", type=str, help="ID of the Experiment Run to enter into the model registry. This option cannot be provided alongside other options, except for --workspace.")
@click.option("--description", "-d", help="Description.")
@click.option("--label", "-l", multiple=True, help="Labels to be associated with the object.")
@click.option("--model", help="Path to the model.")
@click.option("--no-custom-modules", help="Flag to not upload any custom modules.", is_flag=True)
@click.option("--requirements", type=click.Path(exists=True, dir_okay=False), help="Path to the requirements.txt file.")
@click.option("--workspace", "-w", help="Workspace to use.")
@click.pass_context
def create_model_version(ctx, model_name, version_name, label, model, custom_module,
                         no_custom_modules, artifact, workspace, requirements, from_run,
                         description, attribute):
    """Create a new registeredmodelversion entry.
    """
    invalid_from_run_options = (label, model, artifact, requirements)
    if from_run and any(invalid_from_run_options):
        raise click.BadParameter("--from-run cannot be provided alongside other options, except for --workspace")

    client = Client()

    try:
        registered_model = client.get_registered_model(name=model_name, workspace=workspace)
    except ValueError:
        raise click.BadParameter("model {} not found".format(model_name))

    if from_run:
        registered_model.create_version_from_run(run_id=from_run, name=version_name)
        return

    registered_model.create_version(name=version_name, labels=list(label), desc=description)
    # labels have been added
    ctx.invoke(
        update_model_version, model_name=model_name, version_name=version_name, model=model, custom_module=custom_module,
        no_custom_modules=no_custom_modules, artifact=artifact, workspace=workspace, requirements=requirements,
        attribute=attribute
    )
