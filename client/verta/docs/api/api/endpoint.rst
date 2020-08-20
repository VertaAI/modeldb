Endpoint
========

.. note::
    .. versionadded:: 0.15.0

Client
------
.. automethod:: verta.Client.get_or_create_endpoint
.. automethod:: verta.Client.get_endpoint
.. automethod:: verta.Client.create_endpoint
.. automethod:: verta.Client.download_endpoint_manifest

Endpoint
--------
.. autoclass:: verta._deployment.endpoint.Endpoint()
    :members:

Update Strategies
-----------------
.. automodule:: verta.endpoint.update._strategies
    :members:

.. _canary-rules:

Canary Update Rules
^^^^^^^^^^^^^^^^^^^
.. automodule:: verta.endpoint.update.rules
    :members:

Update Configuration
--------------------
Resources
^^^^^^^^^
.. automodule:: verta.endpoint.resources
    :members:

Autoscaling
^^^^^^^^^^^
.. automodule:: verta.endpoint.autoscaling._autoscaling
    :members:
.. automodule:: verta.endpoint.autoscaling.metrics
    :members:

Command Line Interface
----------------------
.. click:: verta._cli.deployment:create
    :prog: verta deployment create
    :show-nested:

.. click:: verta._cli.deployment:get
    :prog: verta deployment get
    :show-nested:

.. click:: verta._cli.deployment:update
    :prog: verta deployment update
    :show-nested:

.. click:: verta._cli.deployment:lst
    :prog: verta deployment list
    :show-nested:

.. click:: verta._cli.deployment:predict
    :prog: verta deployment predict
    :show-nested:
