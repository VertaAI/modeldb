60-Second Runthrough
====================

1. Install the **Verta** Python package.

  .. code-block:: console

    pip install verta

  Verta currently officially supports Python 2.7 & 3.5–3.7. For more information, read the
  `installation guide <installation.html>`_.

2. Log in to the `Verta Web App <https://app.verta.ai>`__.

  .. image:: /_static/images/web-app-login.png
     :width: 50%

  Visit your `profile page <https://app.verta.ai/profile>`__ to find your developer key.

  .. image:: /_static/images/web-app-profile.png
      :width: 50%

  Note that your developer key is unique to you. As with a password, don't share it with others!

3. Integrate the Verta package into your workflow.

  a. Connect to the Verta back end:

    .. code-block:: python

        from verta import Client
        client = Client(host, email, dev_key)

  b. Log things that matter to you:

    .. code-block:: python

        proj = client.set_project("Fraud Detection")
        expt = client.set_experiment("Recurrent Neural Net")

    .. code-block:: python

        run = client.set_experiment_run("Two-Layer Dropout LSTM")

        run.log_hyperparameter("num_layers", 2)
        run.log_hyperparameter("hidden_size", 512)
        run.log_hyperparameter("dropout", 0.5)
        run.log_metric("accuracy", 0.95)

    For more information, read the `workflow guide <workflow.html>`_ and the `API reference
    <../reference/api.html>`_.

3. Now that we've logged a few runs, head to the `Verta Web App <https://app.verta.ai>`__ to view them!
