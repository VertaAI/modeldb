Querying an endpoint
====================

Once the endpoint has finished updating:

.. code-block:: python

    endpoint.update(model_version, strategy, wait=True)


Users can make queries to it. This tutorial will explore several ways to do this, using the client and CLI.

Using the client
----------------

Given an endpoint (which has a model deployed to it), we can make queries as follows:

.. code-block:: python

    deployed_model = endpoint.get_deployed_model()
    results = deployed_model.predict(test_data)

We can also make queries via cURL requests. An example request is given in the string representation of the endpoint object:

.. code-block:: python

    print(endpoint)

    # This should print out similar to:
    # path: <some-path>
    # id: <some-id>
    # curl: curl -X POST <prediction-url> -d '' -H "Content-Type: application/json"
    # ...

Using the CLI
-------------

Verta's CLI also has a ``predict`` command for querying an endpoint:

.. code-block:: sh

    verta deployment predict endpoint /some-path --data '[0, 1, 1, 1]'

The input passed to ``data`` option must have JSON format.
