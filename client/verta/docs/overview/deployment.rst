Deployment & Release
====================

The most challenging and yet crucial operation in operationalization of models is model 
deployment and release.
Due to the diversity of ML frameworks and libraries, and the lack of common systems 
for ML development vs. software delivery systems, it takes many months to release models
into products.

One of Verta's key innovations is our open-core model deployment and release system that 
works seamlessly with models built in over a dozen frameworks and languages, and integrates
with state-of-the-art DevOps and software delivery systems.

Model Deployment and Release in turn includes multiple steps:

* Model Packaging
* Model Deployment
* Model Release

..
    .. warning::
        Add a picture

===============
Model Packaging
===============

Model Packaging involves taking a trained model (in varied formats including weights, checkpoints, pickle
files) and turning it into a runnable format, most often as `containers` or `libraries.`

Verta provides extensive support for packaging models as containers.
By containerizing models and serving inference requests via API calls (REST or gRPC), models can be made
accessible to a variety of other services and applications.

The Verta client provides APIs to package models using a variety of frameworks. Packaging a model consists
of three steps:

* Defining models using the Verta Model Interface (with out of box support for popular frameworks)

    .. code-block:: python

        class MyModel(object):
            def __init__(self, artifacts):
                pass

            def predict(self, data):
                pass

* Uploading relevant artifact dependencies to Verta (e.g., weights, model classes or pickle files) and registering the model class.

    .. code-block:: python

        run.log_artifact("tensorflow_saved_model", "experiment/generated/tensorflow/saved_model/")
        run.log_artifact("nearest_neighbor_index", "experiment/generated/annoy/index.ann")

        run.log_model(
            MyModel,  # class, not instance
            artifacts=["tensorflow_saved_model", "nearest_neighbor_index"],  # logged artifact keys
        )
        run.log_requirements(["annoy", "tensorflow"])

That's it, this model can now be packaged and deployed on the Verta platform. 
Refer to the :doc:`../api/deployment` API reference for details about the Deployment APIs.

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

Refer to the :doc:`../api/deployment` API reference for details about the Deployment APIs.

=============
Model Release
=============

Verta also provides integrations into popular tools such as Jenkins to automate the package and release
of models.
Check out `this <https://github.com/VertaAI/modeldb/tree/master/demos/03-20-mdb-jenkins-prom>`_ Jenkins pipeline to automate the model release process. 

