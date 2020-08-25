Building a model container locally
==================================

Some of our customers want to build a version of the container used for inference locally. Common uses
are debugging and satisfying operations procedures.

Verta supports fetching the whole context necessary for Docker and building locally.

Fetching the Docker context
---------------------------

Any class within the Verta platform that has a ``download_docker_context`` method, such as an
:class:`ExperimentRun <verta._tracking.experimentrun.ExperimentRun>`, automatically supports fetching the Docker
context that can be used to build an image.

For example, you can run

.. code-block:: python

    run.download_docker_context('context.tgz')

and our client will save a file named ``context.tgz`` in your folder with all the contents for the build.

Building the Docker image
-------------------------

Unfortunately Docker doesn't allow you to use a packaged context in your build directly. To unpack
the context you can run

.. code-block:: bash

    mkdir -p context_folder && tar -C context_folder -xvzf context.tgz

which will save the contents to ``context_folder``.

That folder contains all the information required to build an image. You can now run

.. code-block:: bash

    docker build -t mymodel:latest context_folder/

to build the image locally.

.. note::
    You might require permission from your system administrator to access the verified base images
    used for Docker. Please contact your admin or the Verta team at `support@verta.ai <mailto:support@verta.ai>`_.
