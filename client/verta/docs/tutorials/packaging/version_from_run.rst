Creating Model Versions From Experiment Runs
============================================

An alternative to creating model versions `from artifacts <version_from_artifacts.html>`_ is to convert an existing experiment run into a new version for the registered model. This provides a direct link between the experimentation process and production.

This tutorial will explore how this can be done, using the Client and the CLI.

Context
-------

During experimentation, data scientists can use Verta to log the model and deployment-specific information (requirements, custom modules, model API, etc.):

.. code-block:: python

    experiment_run.log_model(model=model, custom_modules=custom_modules, model_api=model_api)
    experiment_run.log_requirements(["torch==1.0.0"])

The models that are good enough for production now needs to be moved to the model registry, as versions of a registered model.

The new versions should automatically inherit requirements, artifacts, model, etc. from the experiment runs they were created from.

Using the Client
----------------

To create a new model version from an existing experiment run, we can use :meth:`RegisteredModel.create_version_from_run() <verta._registry.model.RegisteredModel.create_version_from_run>` method:

.. code-block:: python

    model_version = registered_model.create_version_from_run(
        run_id=experiment_run.id
        name="from-experiment-run"
    )

Using the CLI
-------------

With Verta's CLI, we can create a new model version from an existing experiment run with the ``from-run`` option:

.. code-block:: sh

    verta registry create registeredmodelversion <model name> <version name> --from-run <experiment run id>
