Creating registered models
==========================

After models are trained and evaluated, they need to be stored along with necessary information so that they can be packed and deployed later. Verta's model registry is a centralized store for this purpose.

This tutorial will go through how to create a registered model using the Client and the CLI.

Using the client
----------------

Registered models can be created as follows:

.. code-block:: python

    registered_model = client.create_registered_model(name="my model", labels=["research-purpose", "team-a"])

Notice that we have assigned the labels ``"research-purpose"`` and ``"team-a"`` to the registered model. Labels are great tools for quickly filtering for a list of relevant registered models.

Labels can also be added after the registered model has been created:

.. code-block:: python

    registered_model.add_label("from-scikit-learn")

Using the CLI:
--------------

We can also accomplish the steps above using Verta's command-line interface:

.. code-block:: sh

    verta registry create registeredmodel "my model" -l research-purpose -l team-a

    verta registry update registeredmodel "my model" -l from-scikit-learn

Creating a new version
----------------------

Each registered model can have one or many *model versions* associated with it. Model versions are used to store necessary information for deployment.

Model versions can be `created from artifacts <version_from_artifacts.html>`_, or `converted from an existing experiment run <version_from_run.html>`_.
