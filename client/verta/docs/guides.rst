Guides
======

**Verta**'s client strives to be lightweight and unobtrusive. As an overview:

.. code-block:: python
    :emphasize-lines: 3,7,15,18

    # load data
    data = np.load(DATA_PATH)
    run.log_dataset("data", data)

    # set hyperparameters
    hyperparams = {'C': 1e-3, 'solver': "lbfgs"}
    run.log_hyperparameters(hyperparams)

    # train model
    model = sklearn.linear_model.LogisticRegression(**hyperparams)
    model.fit(data['X_train'], data['y_train'])

    # test model
    test_acc = model.score(data['X_test'], data['y_test'])
    run.log_metric("test accuracy", test_acc)

    # save model
    run.log_model("model", model)


.. toctree::
    :hidden:

    Runthrough <guides/runthrough>
    Package Installation <guides/installation>
    Web App <guides/webapp>
    Workflow Logging <guides/workflow>
    Examples <guides/examples>
    Data Versioning <guides/dataset>
    Class Models <guides/class>
    Workspaces <guides/workspaces>
    Querying <guides/querying>
