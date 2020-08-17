Model Registry in Verta
=======================
In this tutorial, we'll briefly go through the concept of Model Registry, and explore some examples using the Verta client and CLI.

Registered Model
----------------
Conceptually, a *registered model* is a model registered with model registry which can later be packed and deployed.
We can create a registered model in Verta using ``client.create_registered_model()`` as follows:

.. code-block:: python

    registered_model = client.create_registered_model(
        name="my model",
        label=["research-purpose", "team-a"]
    )

We can also create registered models using the CLI:

.. code-block:: sh

    verta registry create registeredmodel "my model" -l research-purpose -l team-a

Model Version
-------------
Each registered model can have one or many *model versions* associated with it.
Each model version stores necessary information for deployment, such as artifacts, model, requirements, etc.

Model versions can be created directly from models and artifacts:

.. code-block:: python

    model_version = registered_model.create_version(
        name="my version",
        labels=["prototype"]
    )

    # Logging the classifier and requirements:

    model_version.log_model(classifier_path)

    reqs = Python.read_pip_file(req_path)
    model_version.log_environment(Python(requirements=reqs))
    model_version.log_environments(Python(requirements=["sklearn"]))

The equivalent CLI command to the snippet above is:

.. code-block:: sh

    verta registry create registeredmodelversion "my model" "my version" \
    --label prototype \
    --model classifier_path \
    --requirements req_path


Model versions can also be created from experiment run, inheriting requirements, artifacts, and models from the run. Using the client:

.. code-block:: python

    model_version = registered_model.create_version_from_run(
        run_id=experiment_run.id
        name="from-experiment-run"
    )

Using the CLI:

.. code-block:: sh

    verta registry create registeredmodelversion "my model" "my version" --from-run experiment-run-id
