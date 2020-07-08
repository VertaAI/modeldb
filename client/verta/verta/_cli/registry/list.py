# -*- coding: utf-8 -*-

import click

from .registry import registry

@registry.group(name="list")
def lst():
    """List all objects available.

    For example, to list all registered models, run

    `verta registry list model`

    or to get all versions of the model `BERT` available, run

    `verta registry list model BERT`
    """
    pass

@lst.command(name="model")
@click.argument("name", nargs=1, required=False)
@click.option("--filter", multiple=True, help="Filters to be applied when listing. See the documentation for a complete list.")
@click.option("--output", "-o", type=click.Choice(['json'], case_sensitive=False), help="Print the output in the given format instead of regular text.")
@click.option("--workspace", "-w", help="Workspace to use")
def lst_model(name, filter, output, workspace):
    """List all models available.
    """
    if name is None:
        print("Listing models")
    else:
        print("Listing versions for model %s" % (name,))
    print("filter: %s" % (filter,))
    print("output: %s" % output)
