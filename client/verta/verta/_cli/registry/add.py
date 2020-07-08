# -*- coding: utf-8 -*-

import click

from .registry import registry

@registry.group(name="add")
def add():
    """Add a new version to an existing entry.

    For example, to create a new model with name BERT, run

    `verta registry create model BERT`
    """

@add.command(name="model")
@click.argument("name", nargs=1, required=True)
@click.argument("label", nargs=1, required=True)
@click.option("--artifact", multiple=True, help="Location of extra artifacts necessary for this model. Use name=location as the argument.")
@click.option("--file", "-f", help="Read version definition from the given file. If missing or \"-\", the input will be used.")
@click.option("--model", help="Location of the serialized model.")
@click.option("--requirements", help="Location of the requirements.txt file for this model.")
@click.option("--tag", "-t", multiple=True, help="Tags to be associated with the object.")
@click.option("--workspace", "-w", help="Workspace to use.")
def add_model(name, label, tag, model, artifact, requirements, workspace):
    """Add a new model version to an existing entry.
    """
    print("Adding model version %s to %s with:" % (label, name))
    print("tags: %s" % (tag,))
    print("model: %s" % (model,))
    print("artifact: %s" % (artifact,))
    print("requirements: %s" % (requirements,))
