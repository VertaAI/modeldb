# Introduction

This is the library for the [scikit-learn](http://scikit-learn.org/stable) client for ModelDB. It is responsible for storing machine learning operations in scikit-learn,
like `LogisticRegression().fit(x_train, y_train)`, in ModelDB. You can explore the [modeldb](modeldb) folder and look through the library.

# Contents

- [Usage](#usage)
    - [Setup](#setup)
    - [Incorporate ModelDB into an existing ML workflow](#incorporate-modeldb-into-an-existing-ml-workflow)
- [Samples](#samples)

# Usage

## Setup
First, make sure you have followed the [setup instructions for ModelDB](../../README.md#setup-and-installation) and have built the client.

**If you installed modeldb via pip, please skip this step, pip has already set the right paths for you.**

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

This assumes that you have an ML workflow that you want to instrument with ModelDB. We only highlight the ModelDB specific steps here. Aside from importing modules and initialization, most of the incorporation includes appending `_sync` to the scikit-learn function names.


### a. Import the ModelDB client library classes

```python
from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableMetrics
```

### b. Create a ModelDB syncer
ModelDBSyncer is the object that logs models and operations to the ModelDB backend. You can initialize the Syncer with your specified configurations as shown below.
Explore the [ModelDBSyncer](modeldb/basic/ModelDbSyncerBase.py) here for more details on the Syncer object and the different ways to initialize it.

<!-- You can initialize the syncer either from a config file (e.g. [syncer.json](https://github.com/mitdbg/modeldb/blob/master/client/syncer.json)) or explicitly via arguments.

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

### c. Log models and pre-processing operations
Next, when you want to log an operation to ModelDB, use the ModelDB **sync** variants of functions by appending `_sync` to the method call. So the original _fit_ calls from scikit-learn would turn into **fit_sync**, _save_ calls would turn into **save_sync** and so on.

```python
x_train, x_test, y_train, y_test = cross_validation.train_test_split_sync(df, target, test_size=0.3)
lr = LogisticRegression()
lr.fit_sync(x_train, y_train) # instead of the usual lr.fit(x_train, y_train)
y_pred = lr.predict_sync(x_test)
```

### d. Log metrics
Use the ModelDB metrics class [**SyncableMetrics**](modeldb/sklearn_native/SyncableMetrics.py).

```python
SyncableMetrics.compute_metrics(model, scoring_function, labels, predictions, dataframe, predictionCol, labelCol)
```
### e. At the end of your workflow, be sure to sync all the data with ModelDB.
```python
 syncer_obj.sync()
```

### f. Run your model
The full code for this example can be found [here](samples/sklearn/SimpleSampleWithModelDB.py). You can also compare it with the code with the original workflow without ModelDB [here](samples/sklearn/SimpleSample.py). Run the sample model and all the model information will be stored in ModelDB.

```bash
python samples/sklearn/SimpleSampleWithModelDB.py
```

You should now be able to [view the model in ModelDB](../../#view-your-models-in-modeldb).

## Samples

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
