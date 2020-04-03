# -*- coding: utf-8 -*-

import click


@click.group()
def code():
    """Create code version blobs."""
    pass


@code.command()
def git():
    """Create Git code blob at location BLOB from current environment."""
    pass
