Access through the client
=========================

Once an organization is created, it can be used to organize access to projects,
datasets, and more.

Verta allows these resources to be contained within organizations—using the
client.

Client and workspaces
---------------------

To specify what organization an resource should be in, methods of
:class:`~verta.client.Client` provide a ``workspace`` parameter where
applicable. For example, to create a project in an organization called
"tutorial":

.. code-block:: python

    client.create_project("Text Classification", workspace="tutorial")

After creation, the project can get retrieved from that workspace:

.. code-block:: python

    client.get_project("Text Classification", workspace="tutorial")

.. image:: /_static/gifs/tutorial-orgs-access.gif
    :width: 90%
    :align: center

By default, these resources are private and need to be
`shared <../sharing.html>`__ with other users in order to enable
collaboration. Client methods also provide a ``public_within_org`` parameter to
make the resource accessible to the entire organization upon creation:

.. code-block:: python

    client.create_project("Image Classification", workspace="tutorial", public_within_org=True)
