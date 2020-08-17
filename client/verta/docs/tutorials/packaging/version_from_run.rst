Creating Model Version From Experiment Run
==========================================
During the experiment process, data scientists can use Verta to log the model and their requirements:
.. code-block:: python

    experiment_run.log_model(model=model, custom_modules=custom_modules, model_api=model_api)
    experiment_run.log_requirements(["torch==1.0.0"])

Now suppose we want to use the model (and requirements) in one such experiment run to create a new version for a registered model. This could be done using the Client as follows:

.. code-block:: python

    model_version = registered_model.create_version_from_run(
        run_id=experiment_run.id
        name="from-experiment-run"
    )

This can also be done via the CLI:

.. code-block:: sh

    verta registry create registeredmodelversion "my model" "from-experiment-run" --from-run experiment-run-id

The new Model Version will automatically inherit requirements, artifacts, and model from the Experiment Run.
