Model Registry
==============

.. note::
    .. versionadded:: 0.15.0

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
    :inherited-members:
