Creating and Retrieving an Endpoint using the Client and CLI
============================================================

Verta's Endpoint provides an API for configurable deployment of machine learning models.

This tutorial will go through how to create and retrieve such an endpoint using Verta Client and CLI.

Using the Client
----------------

Users can create an Endpoint as follows:

.. code-block:: python

    endpoint = client.create_endpoint(path="/some-path")

This Endpoint can be retrieved later on via its ID or its path:

.. code-block:: python

    endpoint = client.get_endpoint(path="/some-path")

    # alternatively:
    endpoint = client.get_endpoint(id="endpoint-id")

Using the CLI
-------------

We can achieve the same task using the CLI:

.. code-block:: sh

    verta deployment create endpoint /some-path

    verta deployment get endpoint /some-path
