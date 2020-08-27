Endpoint resources
==================

Through an endpoint update, you can configure the limits for compute resources available to the
deployment.

Using the client
----------------

:meth:`Endpoint.update() <verta.endpoint.endpoint.Endpoint.update>` provides a parameter for
configuring the endpoint's compute resources. It can be used alongside any update strategy.

.. code-block:: python

    from verta.deployment.update import DirectUpdateStrategy

    endpoint.update(
        model_version, DirectUpdateStrategy(),
        resources=resources,
    )

``resources`` specifies the computational resources that will be available to the model when it is
deployed.

.. code-block:: python

    from verta.endpoint.resources import Resources

    resources = Resources(cpu=.25, memory="512Mi")

In this example, each replica will be provided a fourth of a CPU core and 512
Mi of RAM. For more information about available resources and units, see the
:ref:`update-resources` API documentation.

Using the CLI
-------------

Compute resources can also be configured via the CLI:

.. code-block:: sh

    verta deployment update endpoint /some-path --model-version-id "<id>" \
        --strategy direct \
        --resources '{"cpu": 0.25, "memory": "512Mi"}'

``--resources`` takes a JSON string representing its values. The Python API documentation for
:ref:`update-resources` contains a JSON-equivalent example for the object.
