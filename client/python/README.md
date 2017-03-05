# Introduction

This is the [scikit-learn](http://scikit-learn.org/stable) client
for ModelDB. 

This library is responsible for storing machine learning operations in scikit-learn,
like `LogisticRegression().fit(x_train, y_train)`, in ModelDB. You can explore the [modeldb](modeldb) folder and look through the library.

# Contents

- [Usage](#usage)
<!--     - [Incorporate ModelDB into an existing ML workflow](#incorporate-modeldb-into-an-existing-ml-workflow) -->
- [Samples](#samples)

# Usage
Run:
```bash
./build_client.sh
``` 

Put the python client on your PYTHONPATH:
```
export PYTHONPATH=[path_to_modedb_dir]/client/python:$PYTHONPATH
```
You can also permanently put it on your PYTHONPATH by putting the above line in your `~/.bashrc`. Afterwards, run:
```bash
source ~/.bashrc
```

Or, you can put the following lines in the beginning of your Python files:
```python
import os
import sys
path_to_python_client = "[path_to_modeldb]/client/python"
os.environ["PYTHONPATH"] = path_to_python_client
sys.path.append(path_to_python_client)

```
<!-- 
- [Samples](#samples)
- [Incorporate ModelDB into an existing workflow](#incorporate-modeldb-into-an-existing-ml-workflow) -->

## Incorporate ModelDB into an existing ML workflow
This assumes that you have an ML workflow that you want to instrument with ModelDB. We only highlight the ModelDB specific steps here. Aside from importing modules and initialization, most of the incorporation includes appending `_sync` to the scikit-learn function names.


### a. Import the ModelDB client library classes

```python
from modeldb.sklearn_native import *
from modeldb.sklearn_native.ModelDbSyncer import *

```

### b. Create a ModelDB syncer
ModelDBSyncer is the object that logs models and operations to the ModelDB backend. You can initialize the Syncer as shown below.

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

Explore [ModelDBSyncer](https://github.com/mitdbg/modeldb/blob/master/client/python/modeldb/basic/ModelDbSyncerBase.py) here.

### c. Log models and pre-processing operations
Next use the ModelDB **sync** variants of functions. So _fit_ calls would turn into **fit_sync**, _save_ calls would turn into **save_sync** and so on.


```python
x_train, x_test, y_train, y_test = cross_validation.train_test_split_sync(df, target, test_size=0.3)
lr = LogisticRegression()
lr.fit_sync(x_train, y_train)
y_pred = lr.predict(x_test)
```

### d. Log metrics
Use the ModelDB metrics class (**SyncableMetrics**).

```python
SyncableMetrics.compute_metrics(model, scoring_function, labels, predictions, dataframe, predictionCol, labelCol)
```
<!-- At the end of your workflow, be sure to sync all the data with ModelDB.
```scala
 ModelDbSyncer.sync()
```
-->

**The full code for this example can be found [here](https://github.com/mitdbg/modeldb/blob/master/client/python/samples/sklearn/SimpleSampleWithModelDB.py).** You can also compare it with the code with the original workflow without ModelDB [here](https://github.com/mitdbg/modeldb/blob/master/client/python/samples/sklearn/SimpleSample.py).

### e. Run your program


## Samples

The [samples](samples) folder contains an [sklearn](samples/sklearn) subfolder and a [basic](samples/basic) subfolder.

The scikit-learn examples include common models with the ModelDB workflow incorporated into them. In the basic subfolder, `BasicWorkflow.py` shows ... (TODO) while `BasicWorkflowSyncAll.py` shows how model metadata from a JSON or YAML file can be loaded into ModelDB.


Try running the unittests as:
```bash
python -m unittest discover modeldb/tests/sklearn/
```

Note: unittests have been run with scikit-learn version 0.17.

Try running samples as in:
```bash
python samples/basic/BasicWorkflow.py
```