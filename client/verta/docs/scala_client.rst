Scala client
============

In addition to Python, Verta also has a Scala client.
This allows data scientists who work primarily in Scala to integrate Verta seamlessly into their workflow.

.. TODO: add some links here for more context, or to the Scala API documentation.

The Scala client currently supports Verta's open source functionality. You could use the client to version a dataset on your local machine or AWS S3 bucket, and tracks the metadata of your experiment run.

To add Verta's Scala client to your sbt project, add the following line to the ``build.sbt`` file:

.. TODO: host the client and verifies that this works

.. code-block:: sh

    libraryDependencies += "ai.verta" %% "scala-client" % "0.0.1"

.. TODO: host the API documentation and add links here.

The API documentation can be found here.