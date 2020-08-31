Updating endpoints with canary
==============================

As an alternative to updating an Endpoint `all at once <endpoint_update.html>`__,
Verta offers the ability to roll out a new model incrementally while monitoring
its behavior to prevent a problematic model from being deployed completely.

This is the principle behind a canary update shown in this tutorial.

.. TODO: add link to canary details

Using the client
----------------

Before, a new model was deployed using a direct update strategy. This time, a
:class:`~verta.deployment.update._strategies.CanaryUpdateStrategy` will be used:

.. code-block:: python

    from verta.deployment.update import CanaryUpdateStrategy
    from verta.deployment.update.rules import MaximumRequestErrorPercentageThresholdRule

    strategy = CanaryUpdateStrategy(interval=10, step=0.2)
    strategy.add_rule(MaximumRequestErrorPercentageThresholdRule(0.1))
    endpoint.update(model_version, strategy)  # or endpoint.update(run, strategy)

To perform a canary update, it must be provided with an ``interval`` (in seconds) describing how
often to update the deployment, and a ``step`` (as a ratio between 0 and 1) describing how much of
the deployment should be updated per interval.

A canary update strategy must also have at least one rule associated with it. In this case, the
update will monitor the request error percentage; if it exceeds the threshold we have set (10%),
the rollout will be halted. See the :ref:`canary-rules` API documentation for additional rules
that can be used.

Using the CLI
-------------

As with the direct strategy, a canary update can be performed via the CLI:

.. code-block:: sh

    verta deployment update endpoint /some-path --model-version-id "<id>" \
        --strategy canary \
        --canary-interval 10 \
        --canary-step 0.2 \
        --canary-rule '{"rule": "error_4xx_rate", "rule_parameters": [{"name": "threshold", "value": "0.1"}]}'

``--canary-rule`` takes a JSON string value that represents the rule and threshold to follow.
You can find a reference for accepted values here.

.. TODO: Link to list of JSON values
