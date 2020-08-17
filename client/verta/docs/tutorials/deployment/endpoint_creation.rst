Creating and Retrieving an Endpoint using the Client and CLI
============================================================

Using the Client
----------------

Suppose we want to deploy a machine learning model and expose it for prediction through some endpoint.

The first step would be to create such an Endpoint:

.. code-block:: python

    endpoint = client.create_endpoint(path="/some-path")

This endpoint can be retrieved later on via its ID or its path:

.. code-block:: python

    endpoint = client.get_endpoint(path="/some-path")
    endpoint = client.get_endpoint(id="endpoint-id")

Using the CLI
-------------

We can achieve the same task using the CLI:

.. code-block:: sh

    verta deployment create endpoint /some-path

    verta deployment get endpoint /some-path
