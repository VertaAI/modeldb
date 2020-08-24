Endpoint configuration
======================

Through an endpoint update, you can configure properties of the deployment including its
autoscaling behavior and compute resources.

Using the client
----------------

:meth:`Endpoint.update() <verta._deployment.endpoint.Endpoint.update>` provides parameters for
configuring the resources and behavior of a deployed model. These settings can be used alongside
any update strategy.

.. code-block:: python

    from verta.deployment.update import DirectUpdateStrategy

    endpoint.update(
        model_version, DirectUpdateStrategy(),
        autoscaling=autoscaling,
        resources=resources,
        env_vars=env_vars,
    )

Using the CLI
-------------

Endpoint configuration can also be done via the CLI:

.. code-block:: sh

    verta deployment update endpoint /some-path --model-version-id "<id>" \
        --strategy direct \
        --autoscaling '{"max_replicas": 4, "min_scale": 0.5}' \
        --autoscaling-metric '{"metric": "cpu_utilization", "parameters": [{"name": "target", "value": "0.75"}]}' \
        --resources '{"cpu_millis": 250, "memory": "512Mi"}' \
        --env-vars '{"LOG_LEVEL": "debug"}'

Each argument takes a JSON string representing its respective value. The
Python API documentation contains JSON-equivalent examples for each endpoint
configuration object.
