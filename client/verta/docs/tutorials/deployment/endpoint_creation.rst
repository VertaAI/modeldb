Creating and retrieving an endpoint
===================================

When a machine learning model is released, it must be made available for downstream consumption; Verta offers endpoints as an API for configuring and releasing deployable models.

This tutorial will go through how to create and retrieve such an endpoint using Verta client and CLI.

Using the client
----------------

Users can create an endpoint as follows:

.. code-block:: python

    endpoint = client.create_endpoint(path="/some-path")

This endpoint can be retrieved later on via its path or its ID:

.. code-block:: python

    endpoint = client.get_endpoint(path="/some-path")

    # alternatively:
    endpoint = client.get_endpoint(id=<endpoint id>)

Using the CLI
-------------

We can achieve the same task using the CLI:

.. code-block:: sh

    verta deployment create endpoint /some-path

    verta deployment get endpoint /some-path
