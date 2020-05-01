# -*- coding: utf-8 -*-

import click


@click.group()
def dataset():
    """Create dataset version blobs."""
    pass


@dataset.command()
@click.argument('blob')
@click.argument('paths', nargs=-1)
def path(blob, paths):
    """Create path dataset blob at location BLOB from files in PATHS."""
    if not paths:
        click.echo("no files specified, so no blob created")
        return

    for path in paths:
        click.echo("obtaining metadata about {}").format(path)
    click.echo("writing blob at {}".format(blob))


@dataset.command()
@click.argument('blob')
@click.argument('keys', nargs=-1)
def s3(blob, keys):
    """Create S3 dataset blob at location BLOB from S3 objects in KEYS."""
    if not keys:
        click.echo("no S3 keys specified, so no blob created")
        return

    for key in keys:
        click.echo("obtaining metadata about {}".format(key))
    click.echo("writing blob at {}".format(blob))
