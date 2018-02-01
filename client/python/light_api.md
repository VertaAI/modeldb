# ModelDB's Light API

The Light API is a way for users to incorporate any ML workflow with ModelDB. The code for the API can be found in [ModelDbSyncerBase.py](modeldb/basic/ModelDbSyncerBase.py).

# Contents

- [Usage](#usage)
    - [Setup](#setup)
    - [Incorporate ModelDB into an existing ML workflow](#incorporate-modeldb-into-an-existing-ml-workflow)
- [Samples](#samples)

# Usage

## Setup
First, make sure you have followed the [setup instructions for ModelDB](../../README.md#setup-and-installation) and have built the client.

**If you installed modeldb via pip, you do not need to the following PYTHONPATH; continue to next step**

Next, put the python client on your PYTHONPATH:
```bash
export PYTHONPATH=[path_to_modedb_dir]/client/python:$PYTHONPATH
```
You can also permanently put it on your PYTHONPATH by putting the above line in your `~/.bashrc`. Afterwards, run:
```bash
source ~/.bashrc
```

Or, you can put the following lines in the beginning of your Python files:
```python
import sys
sys.path.append("[path_to_modeldb]/client/python")
```

## Incorporate ModelDB into an existing ML workflow

### a. Import the ModelDB client library class

```python
from modeldb.basic.ModelDbSyncerBase import *
```

### b. Create a ModelDB syncer
ModelDBSyncer is the object that logs models and operations to the ModelDB backend. You can initialize the Syncer with your specified configurations as shown below.
Explore the [ModelDBSyncer](modeldb/basic/ModelDbSyncerBase.py) here for more details on the Syncer object and the different ways to initialize it.

You can initialize the syncer either from a config file (see [the sample config file](../syncer.json)) or explicitly via arguments.

```python
# Initialize syncer from a JSON or YAML config file
syncer_obj = Syncer.create_syncer_from_config(filepath)
# or
# Create a syncer using a convenience API
syncer_obj = Syncer.create_syncer("Sample Project", "test_user", "sample description")
# or
# Create a syncer explicitly
syncer_obj = Syncer(
    NewOrExistingProject("Samples Project", "test_user",
    "using modeldb light logging"),
    DefaultExperiment(),
    NewExperimentRun("", "sha_A1B2C3D4"))
```

### c. Sync Information
- **Method 1**:

    Load all model information from a JSON or a YAML file. The expected key names can be found [here](modeldb/utils/MetadataConstants.py). There are also samples JSON and YAML files in [samples/basic](samples/basic).
    ```python
    syncer_obj.sync_all(filepath)
    syncer_obj.sync()
    ```


- **Method 2**:

    Initialize the `Dataset`, `Model`, `ModelConfig`, `ModelMetrics` classes with the needed information as arguments then call the sync methods on the Syncer object. Finally, call `syncer_obj.sync()`.
    ```python
    # create Datasets by specifying their filepaths and optional metadata
    # associate a tag (key) for each Dataset (value)
    datasets = {
        "train" : Dataset("/path/to/train", {"num_cols" : 15, "dist" : "random"}),
        "test" : Dataset("/path/to/test", {"num_cols" : 15, "dist" : "gaussian"})
    }

    # create the Model, ModelConfig, and ModelMetrics instances
    model = "model_obj"
    model_type = "NN"
    mdb_model1 = Model(model_type, model, "/path/to/model1")
    model_config1 = ModelConfig(model_type, {"l1" : 10})
    model_metrics1 = ModelMetrics({"accuracy" : 0.8})

    # sync the datasets to modeldb
    syncer_obj.sync_datasets(datasets)

    # sync the model with its model config and specify which dataset tag to use for it
    syncer_obj.sync_model("train", model_config1, mdb_model1)

    # sync the metrics to the model and also specify which dataset tag to use for it
    syncer_obj.sync_metrics("test", mdb_model1, model_metrics1)

    syncer_obj.sync()
    ```

The code for the API can be found in [ModelDbSyncerBase.py](modeldb/basic/ModelDbSyncerBase.py), where the `Syncer`, `Dataset`, `Model`, `ModelConfig`, `ModelMetrics` classes and their methods are declared.

### d. Run your model
You should now be able to [view the model in ModelDB](../../#view-your-models-in-modeldb).

## Samples
[BasicWorkflow.py](samples/basic/BasicWorkflow.py) and [BasicSyncAll.py](samples/basic/BasicSyncAll.py) show how ModelDB's Light API can be used. The former shows how each dataset, model, model configuration, and model metrics can be initialized and synced to ModelDB, while the latter shows a simple `sync_all` method where all the data can be imported from a JSON or a YAML file.

Try running samples as in:
```bash
python samples/basic/BasicWorkflow.py
```

