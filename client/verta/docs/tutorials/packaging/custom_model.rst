Creating custom models
======================

Developers frequently need to create a model that is not using a single framework. This mix and match
makes model development easier, but it complicates depending on the model.

Verta supports custom models defined as a class that can perform arbitrary computations and this
tutorial shows you how to create one.

Defining a class model
----------------------

A *Class Model* must implement this interface:

.. code-block:: python

    class MyModel(object):
        def __init__(self, artifacts):
            pass

        def predict(self, data):
            pass

``__init__()`` takes an argument called ``artifacts``, which are covered in `this tutorial
<custom_model_with_dependencies.html>`_.

``predict()`` takes one argument, which is the model input data and returns the model output.
The model input can be of arbitrary type as defined in :meth:`DeployedModel.predict()
<verta.deployment.DeployedModel.predict>`, but it must be serializable to json.

The Verta infrastructure automatically converts known types (arrays, dataframes, and many others) to
their json-compatible format automatically.

In order to use this model, you must also register this model with Verta.

Saving a class model
--------------------

Any class within the Verta platform that has a ``log_model`` method, such as an
:class:`ExperimentRun <verta._tracking.ExperimentRun>`, automatically supports logging
custom models. They will be serialized in the right way for consumption downstream.

For example, when using an experiment run, you can just do

.. code-block:: python

    run.log_model(MyModel)  # class, not instance

and that class will be logged into the system.

When an object of that class is requested, our tools will automatically build one with the right
configuration for you!

More complex models
-------------------

Not every model is self-contained in its definition. We also support `adding runtime artifacts
<custom_model_with_dependencies.html>`_ to models.
