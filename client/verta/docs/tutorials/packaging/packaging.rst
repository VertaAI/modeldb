Packaging
=========

Packaging within the Verta platform helps you collect all the information required to use a model
in different contexts, without having to worry about its dependencies.

Registering the model
^^^^^^^^^^^^^^^^^^^^^

Model registry provides a staging area between models developed during experimentation and models to be taken to production.

The following tutorials go through the steps to add models to the model registry:

* `Creating registered models <create_registered_model.html>`_.
* `Creating model versions from artifacts <version_from_artifacts.html>`_.
* `Creating model versions from experiment runs <version_from_run.html>`_.

Saving the model
^^^^^^^^^^^^^^^^

Before consuming the model, you must first log it into the platform. We recommend checking the
tutorials on `tracking <../tracking/tracking.html>`_ for more details, but here's a summarized list of common
ways to save a model to Verta:

* `Using a custom model <custom_model.html>`_ to perform arbitrary computations.
* `Using a custom model with artifact dependencies <custom_model_with_dependencies.html>`_.

Using the model
^^^^^^^^^^^^^^^

Once a model is within Verta's platform, you can use it in multiple ways. Here are the most common
frequently ones:

* `Building local Docker images <local_docker_build.html>`_ for a logged model.

.. toctree::
    :hidden:
    :titlesonly:

    Custom models <custom_model>
    Custom models with dependencies <custom_model_with_dependencies>
    Building local Docker images <local_docker_build>
    Creating registered models <create_registered_model>
    Creating model versions from artifacts <version_from_artifacts>
    Creating model versions from experiment runs <version_from_run>
