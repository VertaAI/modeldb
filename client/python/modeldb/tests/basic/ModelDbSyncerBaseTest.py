import sys
import modeldb.sklearn_native.ModelDbSyncer
import modeldb.basic.ModelDbSyncerBase as ModelDbSyncerBase


class SyncerTest(ModelDbSyncerBase.Syncer):
    instance = None

    # __new__ always a classmethod
    def __new__(cls, project_config, experiment_config, experiment_run_config):
        # This will break if cls is some random class.
        if not cls.instance:
            cls.instance = object.__new__(
                cls, project_config, experiment_config, experiment_run_config)
            ModelDbSyncer.Syncer.instance = cls.instance
        return cls.instance

    def sync(self):
        events = []
        for b in self.buffer_list:
            event = b.make_event(self)
            events.append(event)
        return events

    def clear_buffer(self):
        self.buffer_list = []
