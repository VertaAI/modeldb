#!/usr/bin/env python

import click

from _init import init

@click.group()
def main():
    pass

main.command()(init)

if __name__ == '__main__':
    main()
