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

Autoscaling
^^^^^^^^^^^

``autoscaling`` takes an :class:`~verta.deployment.autoscaling._autoscaling.Autoscaling` object,
which itself is used to establish upper and lower bounds for the number of replicas running the
model. Autoscaling must also have at least one metric associated with it, which sets a threshold
for triggering a scale-up.

.. code-block:: python

    from verta.deployment.autoscaling import Autoscaling
    from verta.deployment.autoscaling.metrics import CpuUtilizationTarget

    autoscaling = Autoscaling(max_replicas=4, min_scale=0.5)
    autoscaling.add_metric(CpuUtilizationTarget(0.75))

Here, CPU utilization exceeding 75% will lead to more replicas being created. For the full list of
available metrics, see the :ref:`autoscaling` API documentation.

Resources
^^^^^^^^^

``resources`` specifies the computational resources that will be available to the model when it is
deployed.

.. code-block:: python

    from verta.endpoint.resources import Resources

    resources = Resources(cpu_millis=250, memory="512Mi")

In this example, each replica will be provided a fourth of a CPU core and 512 Mi of RAM. For the full
list of available resources, see the :ref:`update-resources` API documentation.

Environment variables
^^^^^^^^^^^^^^^^^^^^^

``env_vars`` takes a dictionary of string environment variable names to string values, and will be
made available to the model when it is deployed.

.. code-block:: python

    env_vars = {'LOG_LEVEL': "debug"}

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

Each argument takes a JSON string representing their respective values. For a reference of accepted
values, see here. To set multiple metrics, ``--autoscaling-metric`` can be provided more than once.

.. TODO Link to list of JSON values
