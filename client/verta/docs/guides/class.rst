Class Models
============

For many users and many relatively small models, pickling and uploading the serialized model object
is sufficient for reproducibility and deployment.

However, there are cases when the model or its framework make serializing the trained model
prohibitively expensive or outright impossible. Alternatively, there may be configuration steps
needed to set up the model on a new machine, which can be a hassle to define within Python's
pickling mechanisms.

For these use cases, you can deploy a **class as a model** through Verta.


Class Model Definition
----------------------

A *Class Model* should implement this interface:

.. code-block:: python

    class Model(object):
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

``predict()`` is identical to other types of user-defined models: it takes one argument and returns
anything you'd like. It is exposed Clientside through :meth:`DeployedModel.predict()
<verta.deployment.DeployedModel.predict>`.


Class Model Logging
-------------------

A *Class Model* also must be linked to its artifacts during logging, so that Verta knows what to
supply it with.

Of course, the artifacts must first be logged.

.. code-block:: python

    run.log_artifact("tensorflow_saved_model", "experiment/generated/tensorflow/saved_model/")
    run.log_artifact("nearest_neighbor_index", "experiment/generated/annoy/index.ann")

Then, the keys of those artifacts must be provided to :meth:`ExperimentRun.log_model()
<verta.client.ExperimentRun.log_model>` as its ``artifacts`` parameter. Now Verta will be able to
provide the deployed model's ``__init__()`` with the ``artifacts`` dictionary it needs.

.. code-block:: python

    run.log_model(
        Model,  # class, not instance
        artifacts=["tensorflow_saved_model", "nearest_neighbor_index"],  # logged artifact keys
    )
    run.log_requirements(["annoy", "tensorflow"])

    run.deploy(wait=True)
    run.get_deployed_model().predict(...)

For local testing, :meth:`ExperimentRun.fetch_artifacts()
<verta.client.ExperimentRun.fetch_artifacts>` returns a dictionary in the aforementioned format
that can be used to initialize a model.

.. code-block:: python

    artifacts = run.fetch_artifacts(["tensorflow_saved_model", "nearest_neighbor_index"])
    model = Model(artifacts=artifacts)
    model.predict(...)


See Also
--------

This walkthrough was loosely based on `this Client example notebook`_ that uses TensorFlow, Annoy,
and Python 2.7 for text embedding and nearest neighbor search.

`This annotated example notebook`_ using TensorFlow Hub and Annoy also takes advantage of *Class
Model*\ s.


.. _this Client example notebook: https://github.com/VertaAI/modeldb/blob/master/client/workflows/demos/Embedding-and-Lookup-TF-Hub.ipynb
.. _This annotated example notebook: https://github.com/VertaAI/modeldb/blob/master/client/workflows/demos/Embedding-and-Lookup-TF-Hub.ipynb
