# Introduction

This is the Python client for ModelDB, which includes ModelDB's Light API and the [scikit-learn](http://scikit-learn.org/stable) client.

Tthe Light API is a way for users to incorporate any ML workflow with ModelDB, while the scikit-learn library is responsible for storing machine learning operations in scikit-learn,
like `LogisticRegression().fit(x_train, y_train)`, in ModelDB. You can explore the [modeldb](modeldb) folder and look through the library.

# Contents

- [Usage](#usage)
    - [Setup](#setup)
    - [Incorporate ModelDB into an existing ML workflow](#incorporate-modeldb-into-an-existing-ml-workflow)
        - [Light API](#light-api)
        - [scikit-learn](#scikit-learn)
- [Samples](#samples)

# Usage

## Setup
First, make sure you have followed the [setup instructions for ModelDB](../../#setup-and-installation) and have built the client.

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

- ### Light API

    #### a. Import the ModelDB client library class

    ```python 
    from modeldb.basic.ModelDbSyncerBase import *
    ```

    #### b. Create a ModelDB syncer
    ModelDBSyncer is the object that logs models and operations to the ModelDB backend. You can initialize the Syncer with your specified configurations as shown below. 
    Explore the [ModelDBSyncer](modeldb/basic/ModelDbSyncerBase.py) here for more details on the Syncer object and the different ways to initialize it.

    You can initialize the syncer either from a config file (see [the sample config file](../scala/libs/spark.ml/syncer.json)) or explicitly via arguments.

    ```python
    # initialize syncer from a JSON or YAML config file
    syncer_obj = Syncer.create_syncer_from_config(filepath)
    # or
    # Create a syncer using a convenience API
    syncer_obj = Syncer.create_syncer("Sample Project", "test_user", "sample description")
    ```

    #### c. Sync Information
    - **Method 1**:
    Initialize the `Dataset`, `Model`, `ModelConfig`, `ModelMetrics` classes with the needed information as arguments then call the methods `sync_datasets`, `sync_model`, and `sync_metrics` on the Syncer object. Finally, call `syncer_obj.sync()`.

    - **Method 2**:
    Load all model infromation from a JSON or a YAML file. The expected key names can be found [here](modeldb/utils/MetadataConstants.py). There are also samples JSON and YAML files in [samples/basic](samples/basic).
    ```python
    syncer_obj.sync_all(filepath)
    syncer_obj.sync()
    ```

    The code for the API can be found in [ModelDbSyncerBase.py](modeldb/basic/ModelDbSyncerBase.py), where the `Syncer`, `Dataset`, `Model`, `ModelConfig`, `ModelMetrics` classes and their methods are declared.

- ### scikit-learn

    This assumes that you have an ML workflow that you want to instrument with ModelDB. We only highlight the ModelDB specific steps here. Aside from importing modules and initialization, most of the incorporation includes appending `_sync` to the scikit-learn function names.


    #### a. Import the ModelDB client library classes

    ```python
    from modeldb.sklearn_native import *
    from modeldb.sklearn_native.ModelDbSyncer import *

    ```

    #### b. Create a ModelDB syncer
    ModelDBSyncer is the object that logs models and operations to the ModelDB backend. You can initialize the Syncer with your specified configurations as shown below. 
    Explore the [ModelDBSyncer](modeldb/basic/ModelDbSyncerBase.py) here for more details on the Syncer object and the different ways to initialize it.

    <!-- You can initialize the syncer either from a config file (e.g. [FIX](https://github.com/mitdbg/modeldb/blob/master/client/scala/libs/spark.ml/syncer.json)) or explicitly via arguments.

    ```python
    # initialize syncer from config file
    FIX.
    ModelDbSyncer.setSyncer(new ModelDBSyncer(SyncerConfig(path_to_config)))
    ```
    OR-->

    ```python
    # initialize syncer explicitly
    syncer_obj = Syncer(
            NewOrExistingProject("proj_name", "username", "proj_description"),
            NewOrExistingExperiment("exp_name", "exp_desc"),
            NewExperimentRun("simple sample test"))
    ```

    #### c. Log models and pre-processing operations
    Next, when you want to log an operation to ModelDB, use the ModelDB **sync** variants of functions by appending `_sync` to the method call. So the original _fit_ calls from scikit-learn would turn into **fit_sync**, _save_ calls would turn into **save_sync** and so on.

    ```python
    x_train, x_test, y_train, y_test = cross_validation.train_test_split_sync(df, target, test_size=0.3)
    lr = LogisticRegression()
    lr.fit_sync(x_train, y_train) # instead of the usual lr.fit(x_train, y_train)
    y_pred = lr.predict(x_test)
    ```

    #### d. Log metrics
    Use the ModelDB metrics class [**SyncableMetrics**](modeldb/sklearn_native/SyncableMetrics.py).

    ```python
    SyncableMetrics.compute_metrics(model, scoring_function, labels, predictions, dataframe, predictionCol, labelCol)
    ```
    <!-- At the end of your workflow, be sure to sync all the data with ModelDB.
    ```scala
     ModelDbSyncer.sync()
    ```
    -->

    #### e. Run your model
    The full code for this example can be found [here](samples/sklearn/SimpleSampleWithModelDB.py). You can also compare it with the code with the original workflow without ModelDB [here](samples/sklearn/SimpleSample.py). Run the sample model and all the model information will be stored in ModelDB.

    ```bash
    python samples/sklearn/SimpleSampleWithModelDB.py
    ```

    You should now be able to [view the model in ModelDB](../../#view-your-models-in-modeldb).

## Samples

- ### Light API
[BasicWorkflow.py](samples/basic/BasicWorkflow.py) and [BasicSyncAll.py](samples/basic/BasicSyncAll.py) show how ModelDB's Light API can be used. The former shows how each dataset, model, model configuration, and model metrics can be initialized and synced to ModelDB, while the latter shows a simple `sync_all` method where all the data can be imported from a JSON or a YAML file.

- ### scikit-learn
The [samples/sklearn](samples/sklearn) folder contains the scikit-learn examples. These include common models with the ModelDB workflow incorporated into them. You may need to install any missing external Python modules used in the samples in order to run them.

Try running samples as in:
```bash
python samples/sklearn/LabelEncoding.py
```

Try running the unittests as:
```bash
python -m unittest discover modeldb/tests/sklearn/
```

Note: unittests have been run with scikit-learn version 0.17.
