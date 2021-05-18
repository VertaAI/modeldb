# -*- coding: utf-8 -*-

def reassign_module(members, module_name):
    for member in members:
        member.__module__ = module_name
