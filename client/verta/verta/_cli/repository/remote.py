# -*- coding: utf-8 -*-

import click

from ..._internal_utils import _config_utils


@click.group()
def remote():
    """Configure remote repositories."""
    pass


@remote.command()
@click.argument('name')
@click.argument('url')
@click.pass_context
def add(ctx, name, url):
    """Add a remote repository."""
    # TODO: validate `url`

    with _config_utils.read_merged_config() as config:
        remotes = config.get('remotes', {})
        if name in remotes:
            raise click.BadParameter("remote '{}' already exists".format(name))

        num_other_remotes = len(remotes)

    with _config_utils.write_local_config() as config:
        config.setdefault('remotes', {})[name] = {
            'url': url,
            'branch': "master",
        }

    if num_other_remotes == 0:
        ctx.invoke(use, name=name)


@remote.command()
@click.argument('name')
def use(name):
    """Set a remote repository as the active one."""
    with _config_utils.read_merged_config() as config:
        if name not in config.get('remotes', {}):
            raise click.BadParameter("no such remote: '{}'".format(name))

    with _config_utils.write_local_config() as config:
        config['current-remote'] = name
