Quickstart
==========

Get started with Verta in 5 minutes.

1. Setup the Verta Server
^^^^^^^^^^^^^^^^^^^^^^^^^

You have a few options here:

* If you are on Verta Core or Verta Enterprise, you do not need to set up a Verta Server; please reach out to `Verta Support <mailto:support@verta.ai>`_ for instructions for your server information.
* You are only looking to run open-source ModelDB, head over to the `ModelDB repo <https://github.com/VertaAI/modeldb>`_ for installation instructions.


2. Setup the Verta Client
^^^^^^^^^^^^^^^^^^^^^^^^^

The Verta client supports Python 2.7 & 3.5–3.7!

  .. code-block:: shell

    # via pip
    pip install verta

    # via conda
    conda install verta -c conda-forge

  *Following Python best practices, we recommended creating a virtual environment using venv or conda.*


3. Obtain your Verta Credentials
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* On Verta Core or Verta Enterprise, log into the Verta Web App (e.g., `https://app.verta.ai <https://app.verta.ai>`__) and visit the Profile page to find your developer key.

    .. image:: /_static/images/web-app-profile.png
        :width: 50%

    Note that your developer key is unique to you. As with a password, don't share it with others!

* If using ModelDB open-source, you will not require any special credentials

4. Integrate the Verta package into your workflow
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  a. Create a Verta client object to connect to the Verta server.

    .. code-block:: python

        from verta import Client
        client = Client(HOST, VERTA_EMAIL, VERTA_DEV_KEY)

    *If using open-source ModelDB, leave `VERTA_EMAIL` and `VERTA_DEV_KEY` blank and set `HOST` to `localhost:3000`*

  b. Version your models

    .. code-block:: python

        proj = client.set_project("Fraud Detection")
        expt = client.set_experiment("Recurrent Neural Net")

    .. code-block:: python

        run = client.set_experiment_run("Two-Layer Dropout LSTM")

        run.log_hyperparameter("num_layers", 2)
        run.log_hyperparameter("hidden_size", 512)
        run.log_hyperparameter("dropout", 0.5)

  c. Associate metadata with your models

    .. code-block:: python

        run.log_metric("accuracy", 0.95)
        run.log_tags(["experiment1"])

5. Check out your models!
^^^^^^^^^^^^^^^^^^^^^^^^^

Now that you have versioned a few models, you can interact with them in a variety of ways:

- Build dashboards on the Verta Web App based on the models
- Merge, branch, and manage all changes to your models
- Share your models and reports with your organization or publicly
- Deploy versioned models via Verta Deployment and Monitoring

..
    For more information, read the `workflow guide <workflow.html>`_ and the `API reference
    <../reference/api.html>`_.
