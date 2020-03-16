Installation
============

The Verta platform consists of two key components: `Verta Client` and the `Verta Server`.

1. Setup the Verta Server
^^^^^^^^^^^^^^^^^^^^^^^^^

You have a few options here:

* If you are on Verta Core or Verta Enterprise, you do not need to set up a Verta Server; please reach out to `Verta Support <mailto:support@verta.ai>`_ for instructions for your server information.
* You are only looking to run open-source ModelDB, head over to the `ModelDB repo <https://github.com/VertaAI/modeldb>`_ for installation instructions.

2. Setup the Verta Client
^^^^^^^^^^^^^^^^^^^^^^^^^

To install the Verta client library, follow the instructions below:

**Verta** supports Python 2.7 & 3.5–3.7!

via pip
-------

It's recommended to first create and activate a virtual environment:

.. code-block:: console

    python -m venv venv
    source venv/bin/activate
    pip install verta


via conda
---------

It's recommended to first create and activate a virtual environment

.. code-block:: console

    conda create -n venv python
    conda activate venv
    conda install verta -c conda-forge

3. Start using Verta
^^^^^^^^^^^^^^^^^^^^^

That's it! Start with a `Tutorial or Example notebooks <examples.html>`_.

Still have questions? Please reach out on our `Slack <support.html>`_ channel.