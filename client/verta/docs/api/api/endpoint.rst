Endpoint
========

Client
------
.. automethod:: verta.Client.get_or_create_endpoint
.. automethod:: verta.Client.get_endpoint
.. automethod:: verta.Client.create_endpoint

Endpoint
--------
.. autoclass:: verta._deployment.endpoint.Endpoint()
    :members:

Update Strategies
-----------------
.. automodule:: verta.deployment.update._strategies
    :members:

Canary Update Rules
^^^^^^^^^^^^^^^^^^^
.. automodule:: verta.deployment.update.rules
    :members:

Update Configuration
--------------------
Resources
^^^^^^^^^
.. automodule:: verta.deployment.resources
    :members:

Autoscaling
^^^^^^^^^^^
.. automodule:: verta.deployment.autoscaling._autoscaling
    :members:
.. automodule:: verta.deployment.autoscaling.metrics
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
