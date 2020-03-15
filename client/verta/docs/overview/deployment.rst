Deployment & Release
====================

The most challenging and yet crucial operation in operationalization of models is model 
deployment and release.
Due to the diversity of ML frameworks and libraries, along with the  disparate systems 
for ML development vs. software delivery systems, it takes many months to release models
into products.

One of Verta's key innovations is our open-core model deployment and release system that 
works seamlessly with models built in over a dozen frameworks and languages, and integrates
with state-of-the-art DevOps and software delivery systems.

Model Deployment and Release in turn includes multiple steps:

* Model Packaging
* Model Deployment
* Model Release

.. warning::
    Add a picture

===============
Model Packaging
===============

Model Packaging involves taking a trained model (in varied formats including weights, checkpoints, pickle
files) and turning it into a runnable format, most often as `containers` or `libraries.`

Verta provides extensive support for packaging models as containers.
By containerizaing models and serving inference requests via API calls (REST or gRPC), models can be made
accessible to a variety of other services and applications.

This process is shown below.

.. warning::
    Add a picture

The Verta client provides APIs to package models using a variety of frameworks. Packaging a model consists
of three steps:

* Defining models using the Verta Model Interface (out of box support for popular frameworks)

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

* Uploading relevant artifacts to Verta (e.g., weights, model classes or pickle files)


    Class Model Logging
    -------------------

    A *Class Model* also must be linked to its artifacts during logging, so that Verta knows what to
    supply it with.

    Of course, the artifacts must first be logged.

    .. code-block:: python

        run.log_artifact("tensorflow_saved_model", "experiment/generated/tensorflow/saved_model/")
        run.log_artifact("nearest_neighbor_index", "experiment/generated/annoy/index.ann")

        run.log_model(
            Model,  # class, not instance
            artifacts=["tensorflow_saved_model", "nearest_neighbor_index"],  # logged artifact keys
        )
        run.log_requirements(["annoy", "tensorflow"])

    Then, the keys of those artifacts must be provided to :meth:`ExperimentRun.log_model()
    <verta.client.ExperimentRun.log_model>` as its ``artifacts`` parameter. Now Verta will be able to
    provide the deployed model's ``__init__()`` with the ``artifacts`` dictionary it needs.

* Calling the package command
    .. code-block:: python

        run.deploy(wait=True)
        run.get_deployed_model().predict(...)

That's it! For custom packaging, check out our `Deployment API reference (fix this) <>`_.

Verta provides integrations into popular tools such as Jenkins to automate the package and release
of models.

*For custom packaging outputs, please contact support@verta.ai.*

================
Model Deployment
================

Once a model has been packaged as a container, Verta provides means to deploy the containerized model
on Kubernetes-based systems.
Verta seamlessly handles the creation of deployments, services, logging and monitoring hooks, and
scaling functionality for any model deployment.
Deployment can be accomplished via the Verta Web App or via Python APIs.

    .. code-block:: python

        run.deploy(wait=True)
        run.get_deployed_model().predict(...)

Refer to the model deployment API reference for details about the Deployment APIs.

=============
Model Release
=============

