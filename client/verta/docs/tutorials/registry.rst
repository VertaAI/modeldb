Model Registry in Verta
=======================
In this tutorial, we'll briefly go through the concept of Model Registry, and explore some examples using the Verta client and CLI.

Registered Model
----------------
Say we have several trained document classification models, stored in ModelDB, and would like to register them for sharing and downstream usage.
We can represent such models in Verta using a *registered model*:

.. code-block:: python

    registered_model = client.create_registered_model(
        name="my model"
        label=["for-research-purpose", "team-a"],
    )

Model Version
-------------
A registered model can contain multiple *model versions*.
Each model version store necessary information for deployment, such as artifacts, model, requirements, etc.

Model versions can be created directly:

.. code-block:: python

    model_version = registered_model.create_version(
        name="my version"
    )

    model_version.log_model(classifier)
    model_version.log_environments(Python(requirements=["sklearn"]))
Model versions can also be created from experiment run, inheriting requirements, artifacts, and models from the run:

.. code-block:: python

    model_version = registered_model.create_version_from_run(
        run_id=experiment_run.id
        name="from-experiment-run"
    )
