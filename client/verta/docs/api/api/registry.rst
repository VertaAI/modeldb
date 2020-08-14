Model Registry
==============

Client
------
.. automethod:: verta.Client.get_or_create_registered_model
.. automethod:: verta.Client.get_registered_model
.. automethod:: verta.Client.create_registered_model
.. automethod:: verta.Client.get_registered_model_version

Registered Model
----------------
.. autoclass:: verta._registry.model.RegisteredModel()
    :members:

Registered Model Version
------------------------
.. autoclass:: verta._registry.modelversion.RegisteredModelVersion()
    :members:

Command Line Interface
----------------------
.. click:: verta._cli.registry:create
    :prog: verta registry create
    :show-nested:

.. click:: verta._cli.registry:get
    :prog: verta registry get
    :show-nested:

.. click:: verta._cli.registry:update
    :prog: verta registry update
    :show-nested:

.. click:: verta._cli.registry:lst
    :prog: verta registry list
    :show-nested:
