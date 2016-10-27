#!/usr/bin/python
import sys
sys.path.append('./thrift/gen-py')
import ModelDbSyncer

# modeldb imports
from events import *
from modeldb import ModelDBService
import modeldb.ttypes as modeldb_types

# sklearn imports
import numpy as np
import pandas as pd
from sklearn.preprocessing import *
from sklearn.linear_model import *


class SyncerTest(ModelDbSyncer.Syncer):
    instance = None
    def __new__(cls, projectConfig, experimentConfig, experimentRunConfig): # __new__ always a classmethod
        # This will break if cls is some random class.
        if not cls.instance:
            cls.instance = object.__new__(cls, projectConfig, experimentConfig, experimentRunConfig)
            ModelDbSyncer.Syncer.instance = SyncerTest.instance    
        return cls.instance
    def sync(self):
        events = []
        for b in self.bufferList:
            event = b.makeEvent(self)
            events.append(event)
        return events
    def clearBuffer(self):
        self.bufferList = []

