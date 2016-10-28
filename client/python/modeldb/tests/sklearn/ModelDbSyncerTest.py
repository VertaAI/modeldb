import sys
import ModelDbSyncer

import modeldb.sklearn.ModelDbSyncer

class SyncerTest(ModelDbSyncer.Syncer):
    def sync(self):
        events = []
        for b in self.bufferList:
            event = b.makeEvent(self)
            events.append(event)
        return events
    
    def clearBuffer(self):
        self.bufferList = []