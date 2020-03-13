Installing the Client
=====================

**Verta** completely and officially supports Python 2.7 & 3.5–3.7!

To check your version of Python:

.. code-block:: console

    python -V

Or if you don't yet have Python, you can install it:

- on Windows by first installing `Chocolatey <https://chocolatey.org/>`_ and then running:

  .. code-block:: console

      choco install python

- on macOS by first installing `Homebrew <https://brew.sh/>`_ and then running:

  .. code-block:: console

      brew install python

- on Linux by running:

  .. code-block:: console

      sudo apt install python


via pip
-------

It's recommended to first create and activate a virtual environment:

.. code-block:: console

    python -m venv venv
    source venv/bin/activate

Then, install **Verta**:

.. code-block:: console

    pip install verta


via conda
---------

It's recommended to first create and activate a virtual environment

.. code-block:: console

    conda create -n venv python
    conda activate venv

Then, install **Verta**:

.. code-block:: console

    conda install verta -c conda-forge
