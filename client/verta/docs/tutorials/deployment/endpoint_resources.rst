Endpoint resources
==================

Using the client
----------------

``resources`` specifies the computational resources that will be available to the model when it is
deployed.

.. code-block:: python

    from verta.endpoint.resources import Resources

    resources = Resources(cpu_millis=250, memory="512Mi")

In this example, each replica will be provided a fourth of a CPU core and 512
Mi of RAM. For more information about available resources and units, see the
:ref:`update-resources` API documentation.

Using the CLI
-------------

.. code-block:: sh

    verta deployment update endpoint /some-path --model-version-id "<id>" \
        --strategy direct \
        --resources '{"cpu_millis": 250, "memory": "512Mi"}'
