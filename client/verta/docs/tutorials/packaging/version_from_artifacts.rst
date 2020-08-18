Creating Model Versions from Artifacts
======================================

With model versions, we can store necessary information for deployment, such as artifacts, model, requirements, etc.

This tutorial will explain how we can create a version from artifacts, using the Client and CLI.

Context
-------

Suppose we have the following LogisticRegression classifier from scikit-learn:

.. code-block:: python

    classifier = LogisticRegression()
    classifier.fit(X_train, y_train)

We need to log this model, along with the requirements to use it (scikit-learn).

Using the Client
----------------

We can create a version for ``registered_model``, and upload the classifier, along with the requirements, as follows:

.. code-block:: python

    model_version = registered_model.create_version(
        name="my version",
        labels=["prototype"]
    )

    # Logging the classifier and requirements:
    model_version.log_model(classifier)

    reqs = Python.read_pip_file(requirements_path)
    model_version.log_environment(Python(requirements=reqs))
    model_version.log_environments(Python(requirements=["sklearn"]))


Using the CLI
-------------

We can also accomplish the steps above using Verta's command-line interface.

First, we need to serialize our classifier. The recommended way to do so is to use the pickle module:

.. code-block:: python

    with open(classifier_path, "wb") as f:
        pickle.dump(classifier, f)

Then, we can create the model version and upload the information (model, requirements, etc.) as follows:

.. code-block:: sh

    verta registry create registeredmodelversion <model name> <version name> \
    --label prototype \
    --model <classifier path> \
    --requirements <requirements path>
