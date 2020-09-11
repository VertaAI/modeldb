Creating model versions from experiment runs
============================================

An alternative to creating model versions `from artifacts <version_from_artifacts.html>`_ is to convert an existing experiment run into a new version of a registered model. This conversion provides a direct link between the experimentation process and production.

In this tutorial, we will explore how this can be done, using the client and the CLI.

Scenario
--------

During experimentation, data scientists would use Verta to log a trained model along with associated artifacts and metadata:

.. code-block:: python

    experiment_run.log_model(model=model, custom_modules=custom_modules, model_api=model_api)
    experiment_run.log_requirements(["torch==1.0.0"])

The models that are good enough for production can now be moved to the model registry, as a version of a registered model.

A model version created from an experiment run will automatically inherit its model, artifacts, Python requirements, and training histogram.

Using the client
----------------

To create a new model version from an existing experiment run, we can use :meth:`RegisteredModel.create_version_from_run() <verta._registry.model.RegisteredModel.create_version_from_run>`:

.. code-block:: python

    model_version = registered_model.create_version_from_run(
        run_id=experiment_run.id,
        name="from-experiment-run",
    )

Using the CLI
-------------

With Verta's CLI, we can create a new model version from an existing experiment run with the ``from-run`` option:

.. code-block:: sh

    verta registry create registeredmodelversion "<model name>" "<version name>" --from-run "<experiment run id>"
