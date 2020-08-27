Logging and querying model metadata
===================================

Setup
-----

First, let's install a machine learning library to work with:

.. code-block:: console

    pip install sklearn

...then launch the Python interpreter:

.. code-block:: console

    python

|

We begin with the :class:`~verta.client.Client`:

.. code-block:: python

    from verta import Client
    client = Client(host, email, dev_key)
    # connection successfully established

``host`` points the client to the Verta back end, ``email`` is the address you have associated
with your GitHub account, and ``dev_key`` is your developer key which you can obtain though the Verta
Web App.

Your email and developer key can also be set using the environment variables ``$VERTA_EMAIL`` and
``$VERTA_DEV_KEY``, so you don't have to explicitly type them into your workflow.
|

Once a client is instantiated and a connection is established, you can create Verta entities to
organize your work:

.. code-block:: python

    proj = client.set_project("Digit Multiclassification")
    # created new Project: Digit Multiclassification
    expt = client.set_experiment("Support Vector Machine")
    # created new Experiment: Support Vector Machine
    run = client.set_experiment_run("RBF Kernel")
    # created new ExperimentRun: RBF Kernel

A *project* is a goal. We're going to classify multiple handwritten digits.

An *experiment* is a strategy for that goal. We'll use a support vector machine as our classification
model.

An *experiment run* is an execution of that strategy. We'll train a support vector machine using the
radial basis function kernel.

Note that you are not restricted to any naming conventions here. Feel free to use names that you
consider useful and meaningful.

If you'd like, you could also add a description, tags, and attributes:

.. code-block:: python

    # run = client.set_experiment_run("Run 1",
    #                                 "SVM w/ RBF kernel",
    #                                 ["example"],
    #                                 {'architecture': "SVM"})


Run tracking
------------

scikit-learn has built-in datasets we can use:

.. code-block:: python

    from sklearn.datasets import load_digits
    digits = load_digits()
    X, y = digits.data, digits.target

We also need to define some hyperparameters to specify a configuration for our model:

.. code-block:: python

    hyperparams = {'kernel': "rbf",
                   'C': 1e-2,
                   'gamma': 0.2}

Then we can finally train a model on our data:

.. code-block:: python

    from sklearn.svm import SVC
    clf = SVC(**hyperparams).fit(X, y)

To see how well we did, we can calculate our mean accuracy on the entire training set:

.. code-block:: python

    train_acc = clf.score(X, y)
    print(train_acc)
    # 0.1018363939899833

|

That's not much better than purely guessing! So how do we keep a more permanent record of this abysmal
*experiment run*? With Verta of course:

.. code-block:: python

    run.log_hyperparameters(hyperparams)
    run.log_model(model)
    run.log_metric("train_acc", train_acc)

|

But logging doesn't need to occur all at once at the end. Let's do another *experiment run* with a
linear kernel—this time interweaving the logging statements with our training process:

.. code-block:: python
    :emphasize-lines: 1,3,5,7

    run = client.set_experiment_run("Linear Kernel")
    hyperparams['kernel'] = 'linear'
    run.log_hyperparameters(hyperparams)
    clf = SVC(**hyperparams).fit(X, y)
    run.log_model(model)
    train_acc = clf.score(X, y)
    run.log_metric("train_acc", train_acc)


Querying
--------

Organizing *experiment run*\ s under *experiment*\ s gives us the ability to retrieve them as a group:

.. code-block:: python

    runs = expt.expt_runs
    runs
    # <ExperimentRuns containing 2 runs>

...and query them:

.. code-block:: python

    best_run = runs.sort("metrics.train_acc", descending=True)[0]
    best_run.get_metric("train_acc")
    # 0.9994435169727324

That's pretty good! So which run was this? Definitely not the RBF kernel:

.. code-block:: python

    best_run.name
    # 'Linear Kernel'


Reproducing
-----------

We can load back the model to see it again for ourselves:

.. code-block:: python

    clf = best_run.get_model()
    clf.score(X, y)
    # 0.9994435169727324

Or we can retrain the model from scratch as a sanity check:

.. code-block:: python

    clf = SVC(**best_run.get_hyperparameters()).fit(X, y)
    clf.score(X, y)
    # 0.9994435169727324
