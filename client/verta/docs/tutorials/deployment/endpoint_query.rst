Query an Endpoint
=================

Using the Client
----------------
Given an Endpoint (which has a model deployed to it), we can make queries as follows:

.. code-block:: python

    deployed_model = endpoint.get_deployed_model()
    results = deployed_model.predict(test_data)

Using the CLI
-------------
Verta's CLI also has a `predict` command for querying an endpoint:

.. code-block:: sh

    verta deployment predict endpoint /some-path --data '[0, 1, 1, 1]'

The input passed to `data` option must have JSON format.