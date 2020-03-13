Clientside Querying
===================

For revisiting past Experiment Runs, the Client offers the ability to find Runs based on their
properties and logged values.

``ExperimentRuns``
------------------

To start, you can obtain a collection of Experiment Runs under a Project or an Experiment using
their ``expt_runs`` attribute:

.. code-block:: python

    proj = client.set_project("Project Banana")
    proj.expt_runs
    # <ExperimentRuns containing 24 runs>
    expt = client.set_project("Experiment Coconut")
    expt.expt_runs
    # <ExperimentRuns containing 8 runs>

These are :class:`~verta.client.ExperimentRuns` objects, which have a method
:meth:`~verta.client.ExperimentRuns.find` for filtering their contents:

.. code-block:: python

    expt.expt_runs
    # <ExperimentRuns containing 8 runs>
    expt.expt_runs.find("metrics.acc > .95")
    # <ExperimentRuns containing 3 runs>
    expt.expt_runs.find("hyperparameters.dropout < .8")
    # <ExperimentRuns containing 4 runs>
    expt.expt_runs.find("hyperparameters.optimizer == 'adam'")
    # <ExperimentRuns containing 6 runs>

Multiple queries can be specified together, which returns the intersection of their results.

.. code-block:: python

    expt.expt_runs.find([
        "metrics.acc > .95",
        "hyperparameters.dropout < .8",
    ])
    # <ExperimentRuns containing 2 runs>

Query Syntax
------------

Queries are Python strings that follow this syntax:

.. code-block:: python

    "<field>.<key> <operator> <value>"

|

For example, lets say I have several Runs, and I've been logging each multi-layer perceptron's
hidden layer size as a hyperparameter with the key "hidden_size".

To find all Runs with a hidden size greater than 512, the query would look like this:

.. code-block:: python

    "hyperparameters.hidden_size > 512"

.. note::

    If the value is a string, it should be surrounded by quotes. As examples:

    .. code-block:: python

        "hyperparameters.optimizer == 'adam'"
        "attributes.team == \"sales\""

Fields
^^^^^^

The fields currently supported by the Client are:

* ``id``
* ``project_id``
* ``experiment_id``
* ``name``
* ``date_created``
* ``attributes``
* ``hyperparameters``
* ``metrics``

.. note::

    ``date_created`` is represented as a Unix timestamp, in milliseconds.

Some fields inherently do not have keys, such as ``id`` and ``name``, in which case their
queries are even more straightforward:

.. code-block:: python

    "<field> <operator> <value>"

For example:

.. code-block:: python

    "name == 'Run Dragonfruit'"

Operators
^^^^^^^^^

The operators currently supported by the Client are:

* ``==``
* ``!=``
* ``>``
* ``>=``
* ``<``
* ``<=``

.. note::

    It is recommended to only use ``==`` and ``!=`` with string values.
