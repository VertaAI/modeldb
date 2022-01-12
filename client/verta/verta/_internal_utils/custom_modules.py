# -*- coding: utf-8 -*-

import pkgutil


class CustomModules(object):

    @staticmethod
    def is_importable(module_name):
        """Return whether `module_name` can be imported.

        Returns
        -------
        bool

        """
        return True if pkgutil.find_loader(module_name) else False
