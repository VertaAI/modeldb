Updating endpoint with a configuration file
===========================================

Verta's client and CLI support updating Endpoint with JSON or YAML configuration file. This tutorial will show how it can be done, using the client and CLI.

Configuration file
------------------

Sometimes, it is more convenient to specify update configuration in a separate, human-readable file, than to `have everything in a Python script or a split CLI command <endpoint_update.html>`_.

For example:

.. code-block:: json

        {
            "run_id": "<experiment run id>",
            "strategy": "canary",
            "canary_strategy": {
                "progress_step": 0.05,
                "progress_interval_seconds": 30,
                "rules": [
                    {"rule": "latency_avg_max", "rule_parameters": [{"name": "threshold", "value": 0.1}]},
                    {"rule": "error_4xx_rate", "rule_parameters": [{"name": "threshold", "value": 1}]}
                ]
            },
            "autoscaling": {
                "quantities": {"min_replicas": 0, "max_replicas": 4, "min_scale": 0.5, "max_scale": 2.0},
                "metrics": [
                    {"metric": "cpu_utilization", "parameters": [{"name": "target", "value": 0.5}]},
                    {"metric": "memory_utilization", "parameters": [{"name": "target", "value": 0.7}]}
                ]
            },
            "env_vars": {"VERTA_HOST": "<Verta host URL>"},
            "resources": {"cpu": 0.25, "memory": "100M"}
        }

.. TODO: Link to configuration file fields.

This allows for easier inspection and modification later on. Users can find out more about the fields of the configuration file here.

Updating via client
-------------------

With the client, instead of using :meth:`Endpoint.update() <verta._deployment.endpoint.Endpoint.update>`, we will use the :meth:`Endpoint.update_from_config() <verta._deployment.endpoint.Endpoint.update_from_config>` method, which takes only the path to the configuration file:

.. code-block:: python

    endpoint.update_from_config("config_file.json")


Updating via the CLI
--------------------

Updating the Endpoint with a config file via the CLI can be done using the ``filename`` option, or ``f`` for short:

.. code-block:: sh

    verta deployment update endpoint /some-path --filename config_file.json

    # Equivalently:
    verta deployment update endpoint /some-path -f config_file.json
