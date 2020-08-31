Creating custom models with dependencies
========================================

Sometimes just `custom models <custom_model.html>`_ by themselves are not enough and you need to add
other data necessary for defining your model behavior.

Verta supports adding artifacts to custom models and this tutorial shows you how to add dependencies
to your model.

Class model definition
----------------------

A *Class Model* must implement this interface:

.. code-block:: python

    class MyModel(object):
        def __init__(self, artifacts):
            pass

        def predict(self, data):
            pass

``__init__()`` takes an argument called ``artifacts``. This is argument a dictionary of artifact keys to
files or directories for those artifacts. As an example:

.. code-block:: python

    artifacts == {
        'tensorflow_saved_model': "/deployment/artifacts/saved_model",
        'nearest_neighbor_index': "/deployment/artifacts/index.ann",
    }

The locations where the artifacts are saved at the time that the model object is initialized are
automatically handled for you.

This dictionary can be used in conjunction with ``open()``, ``json.load()``, and machine learning
frameworks to load data from files. For example, to load a TensorFlow model, you could do:

.. code-block:: python

    def __init__(self, artifacts):
        self.session = tf.Session()
        tf.compat.v1.saved_model.load(
            self.session,
            ['serve'],
            artifacts['tensorflow_saved_model'],  # logged artifact
        )

The keys and values of this dictionary map directly to the arguments used to log such dependencies.

Logging model dependencies
--------------------------

Any class in the Verta platform that has a ``log_artifact`` method, such as an
:class:`ExperimentRun <verta._tracking.experimentrun.ExperimentRun>`,
supports logging dependencies for their associated models. The method signature is generally:

.. code-block:: python

    run.log_artifact("user-defined-name", "location/on/disk/to/artifact")

Please check the documentation for the particular class for more details.

Once you have logged the artifacts into the system, you can log your model and explicitly state the
dependency like

.. code-block:: python

    run.log_model(
        MyModel,  # class, not instance
        artifacts=["tensorflow_saved_model", "nearest_neighbor_index"],  # logged artifact keys
    )

By doing this, whenever an object of ``MyModel`` is instantiated by Verta, the artifacts will be
available for consumption and passed to the constructor automatically.

Local testing
-------------

For local testing, you can use the equivalent method ``fetch_artifacts()`` to fetch the artifacts
and initialize the model.

.. code-block:: python

    artifacts = run.fetch_artifacts(["tensorflow_saved_model", "nearest_neighbor_index"])
    model = Model(artifacts=artifacts)
    model.predict(...)
