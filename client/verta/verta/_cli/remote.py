# -*- coding: utf-8 -*-

import click

from .._internal_utils import _config_utils


@click.group()
def remote():
    """Configure remote repositories."""
    pass


@remote.command()
@click.argument('name')
@click.argument('url')
def add(name, url):
    """Add a remote repository."""
    # TODO: validate `url`

    with _config_utils.read_config() as config:
        if name in config.get('remotes', {}):
            raise click.BadParameter("remote '{}' already exists".format(name))

    with _config_utils.write_config() as config:
        config.setdefault('remotes', {})[name] = {
            'url': url,
            'branch': "master",
        }


@remote.command()
@click.argument('name')
def use(name):
    """Set a remote repository as the active one."""
    with _config_utils.read_config() as config:
        if name not in config.get('remotes', {}):
            raise click.BadParameter("no such remote: '{}'".format(name))

    with _config_utils.write_config() as config:
        config['current-remote'] = name
