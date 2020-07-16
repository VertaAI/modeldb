# -*- coding: utf-8 -*-

import click

from .registry import registry

@registry.group(name="get")
def get():
    """Get detailed information about an object.

    For example, to see information about a registered model named BERT, run

    `verta registry get model BERT`

    or to get the latest BERT model, run

    `verta registry get model BERT latest`

    """
    pass


@get.command(name="registeredmodel")
@click.argument("model_name", nargs=1, required=True)
@click.option("--output", "-o", type=click.Choice(['json'], case_sensitive=False), help="Print the output in the given format instead of regular text.")
@click.option("--workspace", "-w", help="Workspace to use")
def get_model(name, version, output, workspace):
    """Get detailed information about a model.
    """
    if version is None:
        print("Getting details for model %s" % (name,))
    else:
        print("Getting details for version %s of model %s" % (version, name))
    print("output: %s" % output)

@get.command(name="registeredmodelversion")
@click.argument("model_name", nargs=1, required=True)
@click.argument("version_name", nargs=1, required=False)
@click.option("--output", "-o", type=click.Choice(['json'], case_sensitive=False), help="Print the output in the given format instead of regular text.")
@click.option("--out-dir", help="Folder to download all the artifacts to.")
@click.option("--workspace", "-w", help="Workspace to use")
def get_model_version(name, version, output, workspace):
    """Get detailed information about a model.
    """
    if version is None:
        print("Getting details for model %s" % (name,))
    else:
        print("Getting details for version %s of model %s" % (version, name))
    print("output: %s" % output)
