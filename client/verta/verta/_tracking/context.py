# -*- coding: utf-8 -*-

class _Context(object):
    def __init__(self, conn):
        self._conn = conn
        self.workspace_name = None
        self.proj = None
        self.expt = None
        self.expt_run = None

    # TODO
    def populate(self):
        pass
