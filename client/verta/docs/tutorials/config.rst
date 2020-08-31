Client config file
==================
The Client config file allows you to define settings that are easily repeatable and sharable. This
can be either a `JSON <https://www.json.org/json-en.html>`__ or a `YAML <https://yaml.org/>`__ file.

When the :class:`~verta.client.Client` is instantiated, it will look for a file called
``verta_config.json`` or ``verta_config.yaml`` in the following locations—in order—using the
first one it finds:

1. the current directory
2. parent directories, recursively
3. ``$HOME/.verta/``

Example
-------
Defining a config file like this:

``verta_config.yaml``

.. code-block:: yaml

    email: "hello@verta.ai"
    dev_key: "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
    host: "app.verta.ai"
    project: "My Project"
    experiment: "My Experiment"

allows you go directly from instantiating a client to working with your
:class:`~verta._tracking.experimentrun.ExperimentRun` without needing to explicitly set the project or experiment in
your code:

``workflow.py``

.. code-block:: python

    from verta import Client

    client = Client()
    run = client.set_experiment_run("New Run")
    # setting email from config file
    # setting dev_key from config file
    # setting host from config file
    # connection successfully established
    # setting experiment from config file
    # setting project from config file
    # set existing Project: My Project from personal workspace
    # set existing Experiment: My Experiment
    # created new ExperimentRun: New Run

Supported fields
----------------
For the fields and structure of the config file itself, please refer to the specification `here
<https://github.com/VertaAI/modeldb/blob/master/protos/protos/public/client/Config.proto>`__.
