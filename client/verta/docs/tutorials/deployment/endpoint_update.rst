Model deployment
================

After an endpoint has been `created <endpoint_creation.html>`__, it needs to be updated with a
machine learning model that can serve predictions.

This tutorial demonstrates how to take a model you've logged with Verta and deploy it to an
Endpoint.

Using the client
----------------

Whether the endpoint is already live and serving predictions, or was newly created moments ago, the
process for updating it with a new model is the same:

.. code-block:: python

    from verta.deployment.update import DirectUpdateStrategy

    endpoint.update(model_version, DirectUpdateStrategy())

The first argument to :meth:`Endpoint.update() <verta.endpoint._endpoint.Endpoint.update>` is your
:class:`~verta._registry.modelversion.RegisteredModelVersion` that already has a trained model and
its Python environment logged.

The second argument is a strategy to use for the update. Here, you would be using a simple
:class:`~verta.deployment.update._strategies.DirectUpdateStrategy` that will fully transition the
endpoint to use your new model.

You can also update an endpoint with an :class:`~verta._tracking.experimentrun.ExperimentRun`:

.. code-block:: python

    endpoint.update(run, DirectUpdateStrategy())

Using the CLI
-------------

The same action can be done through the CLI. Again, using a model version and a direct update:

.. code-block:: sh

    verta deployment update endpoint /some-path --run-id "<id>" --strategy direct

Or using an experiment run:

.. code-block:: sh

    verta deployment update endpoint /some-path --model-version-id "<id>" --strategy direct

Advanced endpoint updates
-------------------------
Certain properties of the endpoint can also be :ref:`configured <endpoint-config>` during the
update, such as compute resources and metric-based autoscaling.

Alternatively, an endpoint can be updated in a more `incremental, rule-based manner
<endpoint_canary_update.html>`__ rather than all at once.
