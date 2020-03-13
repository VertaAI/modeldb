Working in Workspaces
=====================

Verta offers Workspaces as an enterprise feature to enable sharing, collaboration, and permissions.

By default, the Client will set Projects and Datasets in your personal Workspace. You can also
manually specify another Workspace—for a collaborative organization you're a part of, or a colleague
who has shared a Project with you.

To do this, a ``workspace`` parameter is available in both
:meth:`Client.set_project() <verta.client.Client.set_project>` and
:meth:`Client.set_dataset() <verta.client.Client.set_dataset>`:

.. code-block:: python

    client.set_project("Project Banana")
    # created new Project: Project Banana in personal workspace
    # <Project "Project Banana">
    client.set_project("Project Coconut", workspace="Organization-Coconut")
    # created new Project: Project Coconut in workspace: Organization-Coconut
    # <Project "Project Coconut">
