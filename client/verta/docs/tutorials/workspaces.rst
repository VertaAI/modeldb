Working in workspaces
=====================

Verta offers workspaces as a feature in Verta Core and Verta Enterprise.
Workspaces enable sharing, collaboration, and permissions.

By default, projects, datasets, and other entities are created in your personal workspace.
You can specify another workspace when initializing Verta to ensure that all the work you do
is performed in that workspace.

For example, the ``workspace`` parameter is available in both
:meth:`Client.set_project() <verta.client.Client.set_project>` and
:meth:`Client.set_dataset() <verta.client.Client.set_dataset>` and can be used as follows:

.. code-block:: python

    client.set_project("Sentiment Classification")
    # created new Project: Sentiment Classification in Personal workspace
    # <Project "Sentiment Classification">
    client.set_project("Sentiment Classification", workspace="Acme-Corp")
    # created new Project: Sentiment Classification in workspace: Acme-Corp
    # <Project "Sentiment Classification">
