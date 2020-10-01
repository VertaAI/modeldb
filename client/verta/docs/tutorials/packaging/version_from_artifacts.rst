Creating model versions from artifacts
======================================

Once `a registered model has been created <create_registered_model.html>`_, we can add new versions to it. With model versions, we can store necessary information for deployment, such as artifacts, model, requirements, etc.

This tutorial will explain how we can create a version from artifacts, using the client and CLI.

Using the client
----------------

Suppose we have just trained the following LogisticRegression classifier from scikit-learn:

.. code-block:: python

    classifier = LogisticRegression()
    classifier.fit(X_train, y_train)

We can create a new version for the registered model, and upload the classifier, along with the requirements (scikit-learn), as follows:

.. code-block:: python

    model_version = registered_model.create_version(
        name="my version",
        labels=["prototype"],
    )

    # Logging the classifier and requirements:
    model_version.log_model(classifier)

    model_version.log_environment(Python(requirements=["scikit-learn"]))

Using the CLI
-------------

We can also accomplish the steps above using Verta's command-line interface.

First, we need to serialize our classifier, for example via ``pickle``:

.. code-block:: python

    with open(classifier_path, "wb") as f:
        pickle.dump(classifier, f)

Then, we can create the model version and upload the information (model, requirements, etc.) as follows:

.. code-block:: sh

    verta registry create registeredmodelversion "<model name>" "<version name>" \
        --label prototype \
        --model "<classifier path>" \
        --requirements "<requirements path>"
