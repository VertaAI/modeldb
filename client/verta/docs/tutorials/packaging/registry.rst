Register Models and Versions in Model Registry
==============================================
After models are trained and evaluated, they need to be ''stored'' along with necessary information so that they can be packed and deployed later.
This tutorial will show how this can be done using Verta's Model Registry.

Creating a registered model and model version using the Client
--------------------------------------------------------------
Suppose we have the following LogisticRegression classifier from scikit-learn:

.. code-block:: python

    classifier = LogisticRegression()
    classifier.fit(X_train, y_train)

We need to log this model, along with the requirements to use it (scikit-learn).
The first step is to create a registered model in Verta using ``client.create_registered_model()`` as follows:

.. code-block:: python

    registered_model = client.create_registered_model(name="my model", labels=["research-purpose", "team-a"])

Each registered model can have one or many *model versions* associated with it.
With model versions, we can store necessary information for deployment, such as artifacts, model, requirements, etc. as follows:

.. code-block:: python

    model_version = registered_model.create_version(
        name="my version",
        labels=["prototype"]
    )
    
    # Logging the classifier and requirements:
    model_version.log_model(classifier)

    reqs = Python.read_pip_file(req_path)
    model_version.log_environment(Python(requirements=reqs))
    model_version.log_environments(Python(requirements=["sklearn"]))


Creating a registered model and model version using the CLI:
------------------------------------------------------------

We can also accomplish the steps above using Verta's command-line interface.
First, we need to serialize our machine learning model. The recommended way to do so is to use the pickle module:

.. code-block:: python

    with open(classifier_path, "wb") as f:
        pickle.dump(classifier, f)

Then, we can create a registered model and a model version as follows:

.. code-block:: sh

    verta registry create registeredmodel "my model" -l research-purpose -l team-a

    verta registry create registeredmodelversion "my model" "my version" \
    --label prototype \
    --model classifier_path \
    --requirements req_path

