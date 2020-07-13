# -*- coding: utf-8 -*-

from .experiment import Experiment
from .project import Project

class _Context(object):
    def __init__(self, conn, conf):
        self._conn = conn
        self._conf = conf
        self.workspace_name = None
        self.proj = None
        self.expt = None
        self.expt_run = None

    def populate(self):
        if self.expt is None and self.expt_run is not None:
            self.expt = Experiment._get_by_id(self._conn, self._conf, self.expt_run.msg.experiment_id)
        if self.proj is None and self.expt is not None:
            self.proj = Project._get_by_id(self._conn, self._conf, self.expt.msg.project_id)
