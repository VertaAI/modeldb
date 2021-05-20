# -*- coding: utf-8 -*-

from .entities import Experiment
from .entities import Project


class _Context(object):
    def __init__(self, conn, conf):
        self._conn = conn
        self._conf = conf

        self.workspace_name = None

        self.proj = None
        self.expt = None
        self.expt_run = None

    def populate(self):
        # TODO: check if the upper entity is already correct, in which case don't re-populate
        if self.expt_run is not None:
            self.expt = Experiment._get_by_id(self._conn, self._conf, self.expt_run._msg.experiment_id)
        if self.expt is not None:
            self.proj = Project._get_by_id(self._conn, self._conf, self.expt._msg.project_id)
