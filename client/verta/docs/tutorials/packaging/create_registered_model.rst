Creating Registered Models
==========================

After models are trained and evaluated, they need to be stored along with necessary information so that they can be packed and deployed later. Verta's model registry is a centralized store for this purpose.

This tutorial will go through how to create a register model using the Client and the CLI.

Using the Client
----------------

Registered models can be created as follows:

.. code-block:: python

    registered_model = client.create_registered_model(name="my model", labels=["research-purpose", "team-a"])

Using the CLI:
--------------

We can also accomplish the step above using Verta's command-line interface:

.. code-block:: sh

    verta registry create registeredmodel "my model" -l research-purpose -l team-a

Creating a new version
----------------------

Each registered model can have one or many *model versions* associated with it. Model versions are used to store necessary information for deployment.

Model versions can be `created from artifacts <version_from_artifacts.html>`_, or `converted from an existing experiment run <version_from_run.html>`_.
