Endpoint environment variables
==============================

Using the client
----------------

``env_vars`` takes a dictionary of string environment variable names to string values, and will be
made available to the model when it is deployed.

.. code-block:: python

    env_vars = {'LOG_LEVEL': "debug"}

Using the CLI
-------------

.. code-block:: sh

    verta deployment update endpoint /some-path --model-version-id "<id>" \
        --strategy direct \
        --env-vars '{"LOG_LEVEL": "debug"}'
