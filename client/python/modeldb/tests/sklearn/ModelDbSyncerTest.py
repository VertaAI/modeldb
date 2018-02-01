import modeldb.sklearn_native.ModelDbSyncer as ModelDbSyncer
from modeldb.utils.Singleton import Singleton
from future.utils import with_metaclass


class SyncerTest(with_metaclass(Singleton, ModelDbSyncer.Syncer)):

    # Singleton. If an ModelDbSyncer.Syncer instance already exists
    # a new one is instantiated, and the old one is overwritten

    def sync(self):
        events = []
        for b in self.buffer_list:
            event = b.make_event(self)
            events.append(event)
        return events

    def clear_buffer(self):
        self.buffer_list = []
