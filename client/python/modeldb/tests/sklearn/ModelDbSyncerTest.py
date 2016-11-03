import sys
import modeldb.sklearn_native.ModelDbSyncer as ModelDbSyncer

class SyncerTest(ModelDbSyncer.Syncer):
    instance = None
    def __new__(cls, project_config, experiment_config, experiment_run_config): # __new__ always a classmethod
        # This will break if cls is some random class.
        if not cls.instance:
            cls.instance = object.__new__(cls, project_config, experiment_config, experiment_run_config)
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