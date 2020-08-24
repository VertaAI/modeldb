Endpoint environment variables
==============================

Through an endpoint update, you can assign values to environment variables that will be exposed to
the deployment.

Using the client
----------------

:meth:`Endpoint.update() <verta.endpoint.endpoint.Endpoint.update>` provides a parameter for
setting the endpoint's environment variables. It can be used alongside any update strategy.

.. code-block:: python

    from verta.deployment.update import DirectUpdateStrategy

    endpoint.update(
        model_version, DirectUpdateStrategy(),
        env_vars=env_vars,
    )

``env_vars`` takes a dictionary of string environment variable names to string values, and will be
made available to the model when it is deployed.

.. code-block:: python

    env_vars = {'LOG_LEVEL': "debug"}

Using the CLI
-------------

Environment variables can also be set via the CLI:

.. code-block:: sh

    verta deployment update endpoint /some-path --model-version-id "<id>" \
        --strategy direct \
        --env-vars '{"LOG_LEVEL": "debug"}'

``--env-vars`` takes a JSON string containing a dictionary of string environment variable names
to string values.
