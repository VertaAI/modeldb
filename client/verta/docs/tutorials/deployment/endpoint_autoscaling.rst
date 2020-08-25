Endpoint autoscaling
====================

Through an endpoint update, you can configure the deployment's autoscaling behavior: upper and
lower bounds for replication, scale-up rate, and metrics to trigger it.

Using the client
----------------

:meth:`Endpoint.update() <verta.endpoint.endpoint.Endpoint.update>` provides a parameter for
configuring the endpoint's autoscaling behavior. It can be used alongside any update strategy.

.. code-block:: python

    from verta.deployment.update import DirectUpdateStrategy

    endpoint.update(
        model_version, DirectUpdateStrategy(),
        autoscaling=autoscaling,
    )

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
available metrics, see the :ref:`autoscaling-metrics` API documentation.

Using the CLI
-------------

Autoscaling can also be configured via the CLI:

.. code-block:: sh

    verta deployment update endpoint /some-path --model-version-id "<id>" \
        --strategy direct \
        --autoscaling '{"max_replicas": 4, "min_scale": 0.5}' \
        --autoscaling-metric '{"metric": "cpu_utilization", "parameters": [{"name": "target", "value": "0.75"}]}'

``--autoscaling`` takes a JSON string representing the quantites for replicas and scale;
``--autoscaling-metric`` takes a JSON string representing a metric and its values. The Python API
documentation for :ref:`autoscaling` and :ref:`autoscaling-metrics` contain JSON-equivalent
examples for each object.

To set multiple metrics, ``--autoscaling-metric`` can be provided more than
once.
