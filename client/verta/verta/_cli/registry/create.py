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

@create.command(name="model")
@click.argument("name", nargs=1, required=True)
@click.option("--tag", "-t", multiple=True, help="Tags to be associated with the object.")
@click.option("--visibility", "-v", default="private", show_default=True, type=click.Choice(["private", "org"], case_sensitive=False), help="Visibility level of the object.")
@click.option("--workspace", "-w", help="Workspace to use.")
def create_model(type, name, tag, visibility, workspace):
    """Create a new model entry.
    """
    print("Creating resource of type %s and name %s with:" % (type, name))
    print("tags: %s" % (tag,))
    print("visibility: %s" % visibility)
