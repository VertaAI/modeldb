Versioning
==========
ExperimentRun
-------------
See ``ExperimentRun``'s :ref:`experiment-run-versioning` section.

Repository
----------
.. automethod:: verta.Client.get_or_create_repository
.. autoclass:: verta._repository.Repository
    :members:
.. autoclass:: verta._repository.commit.Commit
    :members:

.. _blobs:

Blobs
-----
Code
^^^^
.. autoclass:: verta.code._git.Git
    :members:
.. autoclass:: verta.code._notebook.Notebook
    :members:

Configuration
^^^^^^^^^^^^^
.. autoclass:: verta.configuration._hyperparameters.Hyperparameters
    :members:

Dataset
^^^^^^^
.. autoclass:: verta.dataset.Path
    :members:
    :inherited-members:
.. autoclass:: verta.dataset.S3
    :members:
    :inherited-members:
.. autoclass:: verta.dataset._dataset.Component()
    :members:

Environment
^^^^^^^^^^^
.. autoclass:: verta.environment.Python
    :members:
