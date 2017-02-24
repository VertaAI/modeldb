# Getting Started with ModelDB on scikit_learn

## 1. Clone the repo

```git
git clone https://github.com/mitdbg/modeldb
```

## 2. Install dependencies
We assume that you have Python and scikit-learn installed.

**Versions**: ModelDB currently requires **Thrift 0.9.3 or 0.10.0**, **Python 2.7**, and **scikit-learn 0.17**.

On Mac OSX:

```bash
brew install sqlite
brew install maven
brew install node
brew install mongodb

# ModelDB works with Thrift 0.9.3 and 0.10.0. If you do not have thrift installed, install via brew.

brew install thrift
```



On Linux:

```bash
apt-get update
sudo apt-get install sqlite
sudo apt-get install maven
sudo apt-get install nodejs # may need to symlink node to nodejs. "cd /usr/bin; ln nodejs node"
sudo apt-get install -y mongodb-org # further instructions here: https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/

# install thrift. path_to_thrift is the installation directory
cd path_to_thrift
wget http://mirror.cc.columbia.edu/pub/software/apache/thrift/0.9.3/thrift-0.9.3.tar.gz
tar -xvzf thrift-0.9.3.tar.gz
cd thrift-0.9.3
./configure
make
export PATH=path_to_thrift/:$PATH
```
For more help on installing dependencies see [here](https://github.com/mitdbg/modeldb/blob/master/docs/required_software.md).

## 3. Build

ModelDB is composed of three components: the ModelDB server, the ModelDB client libraries, and the ModelDB frontend.

In the following, **path_to_modeldb** refers to the directory into which you have cloned the modeldb repo and **thrift_version** is 0.9.3 or 0.10.0 depending on your thrift version (check by running ```thrift -version```).

```bash
# build and start the server
cd path_to_modeldb/server
cd codegen
./gen_sqlite.sh
cd ..
./start_server.sh thrift_version &

# build scikit-learn client library
cd path_to_modeldb/client/python
./build_client.sh

# start the frontend
cd path_to_modeldb/frontend
./start_frontend.sh &

# shutdown mongodb server after killing the server
# on Mac OSX and Linux
mongo --eval "db.getSiblingDB('admin').shutdownServer()" 
```

## 4. Incorporate ModelDB into an ML workflow
This assumes that you have an ML workflow that you want to instrument with ModelDB. We only highlight the ModelDB specific steps here.

#### a. Import the ModelDB client library classes

```python
from modeldb.sklearn_native import *
from modeldb.sklearn_native.ModelDbSyncer import *

```

#### b. Create a ModelDB syncer
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

#### c. Log models and pre-processing operations
Next use the ModelDB **sync** variants of functions. So _fit_ calls would turn into **fit_sync**, _save_ calls would turn into **save_sync** and so on.


```python
x_train, x_test, y_train, y_test = cross_validation.train_test_split_sync(df, target, test_size=0.3)
lr = LogisticRegression()
lr.fit_sync(x_train, y_train)
y_pred = lr.predict(x_test)
```

#### d. Log metrics
Use the ModelDB metrics class (**SyncableMetrics**).

```python
SyncableMetrics.compute_metrics(model, scoring_function, labels, predictions, dataframe, predictionCol, labelCol)
```
<!-- At the end of your workflow, be sure to sync all the data with ModelDB.
```scala
 ModelDbSyncer.sync()
```
-->

**The full code for this example can be found [here](https://github.com/mitdbg/modeldb/blob/master/client/python/samples/sklearn/SimpleSample.py).**

#### e. _Run your program!_

Be sure to add the modeldb python client folder to your path.

```
export PYTHONPATH=path_to_modedb_dir/client/python:$PYTHONPATH
```

## 5. Explore models
That's it! Explore the models you built in your workflow at [http://localhost:3000](http://localhost:3000).

<img src="images/frontend-1.png">



**More complex scikit-learn workflows using ModelDB are located [here](https://github.com/mitdbg/modeldb/tree/master/client/python/samples/sklearn).**
