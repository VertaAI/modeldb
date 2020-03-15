Deeploying Custom  Models
=========================

If you are deploying a model that is not purely a scikit-learn, xgboost, Tensorflow or PyTorch model,
Verta provides a simple means to package any model for deployment.

For such cases, Verta provides an interface that model classes must implement.

Class Model Definition
----------------------

A *Class Model* should implement this interface:

.. code-block:: python

    class MyModel(object):
        def __init__(self, artifacts):
            pass

        def predict(self, data):
            pass

``__init__()`` takes an argument called ``artifacts``. This is a dictionary of artifact keys to
files or directories for those artifacts. As an example:

.. code-block:: python

    artifacts == {
        'tensorflow_saved_model': "/deployment/artifacts/saved_model",
        'nearest_neighbor_index': "/deployment/artifacts/index.ann",
    }

This dictionary can be used in conjunction with ``open()``, ``json.load()``, and machine learning
frameworks to load data from files. For example:

.. code-block:: python

    def __init__(self, artifacts):
        self.session = tf.Session()
        tf.compat.v1.saved_model.load(
            self.session,
            ['serve'],
            artifacts['tensorflow_saved_model'],  # logged artifact
        )

As you may have noticed, the values in this dictionary don't matter too much when writing your
model. ``__init__()`` only needs to care about indexing the dictionary items by the keys of the
artifacts; Verta will take care of the values when the model is deployed.

``predict()`` takes one argument, which is the model input data and returns the model output. 
The model input can be of arbitrary type as defined in :meth:`DeployedModel.predict()
<verta.deployment.DeployedModel.predict>`.

In order to deploy this model, one must also register this model and dependencies with Verta.

Class Model Logging
-------------------

First, we log the dependencies (or artifacts) that the model depends on.
For example, if a model depends on a tensorflow saved model and a nearest neighbor index,
it can be registered as follows.
This call will store the dependencies on Verta and make them available with this run.

.. code-block:: python

    run.log_artifact("tensorflow_saved_model", "experiment/generated/tensorflow/saved_model/")
    run.log_artifact("nearest_neighbor_index", "experiment/generated/annoy/index.ann")

Next, we register the model as defined above and provide the keys of the dependencies via
:meth:`ExperimentRun.log_model()<verta.client.ExperimentRun.log_model>`.

.. code-block:: python

    run.log_model(
        MyModel,  # class, not instance
        artifacts=["tensorflow_saved_model", "nearest_neighbor_index"],  # logged artifact keys
    )
    # any special library requirements
    run.log_requirements(["annoy", "tensorflow"])

For local testing, use :meth:`ExperimentRun.fetch_artifacts()
<verta.client.ExperimentRun.fetch_artifacts>` to fetch the artifact dependencies and initialize the
model.

.. code-block:: python

    artifacts = run.fetch_artifacts(["tensorflow_saved_model", "nearest_neighbor_index"])
    model = Model(artifacts=artifacts)
    model.predict(...)

To deploy a model registered as shown above, use these two calls:

.. code-block:: python

    run.deploy(wait=True)
    run.get_deployed_model().predict(...)


See Also
--------

This walkthrough was loosely based on `this example`_ that uses TensorFlow, Annoy,
and Python 2.7 for text embedding and nearest neighbor search.

`This annotated example notebook`_ using TensorFlow Hub and Annoy also takes advantage of *Class
Model*\ s.


.. _this Client example notebook: https://github.com/VertaAI/modeldb/blob/master/client/workflows/demos/Embedding-and-Lookup-TF-Hub.ipynb
.. _This annotated example notebook: https://github.com/VertaAI/modeldb/blob/master/client/workflows/demos/Embedding-and-Lookup-TF-Hub.ipynb
