# ModelDB Light API

[BasicWorkflow.py](BasicWorkflow.py) and [BasicSyncAll.py](BasicSyncAll.py) show how ModelDB's Light API can be used. The former shows how each dataset, model, model configuration, and model metrics can be initialized and synced to ModelDB, while the latter shows a simple `sync_all` method where all the data can be imported from a JSON or a YAML file.

The code for the API can be found in [ModelDbSyncerBase.py](../../modeldb/basic/ModelDbSyncerBase.py), where the `Syncer`, `Dataset`, `Model`, `ModelConfig`, `ModelMetrics` classes and their methods are declared.
