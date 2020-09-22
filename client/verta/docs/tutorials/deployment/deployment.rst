Deployment
==========

After the models are trained and packaged, it can be deployed for downstream consumption.

The following tutorials will walk you through the entire process of using an endpoint, Verta's configurable API for deploying machine learning models.

Endpoint creation
-----------------

* `Creating and retrieving an endpoint <endpoint_creation.html>`__

Endpoint update
---------------

* `Updating an endpoint <endpoint_update.html>`__
* `Updating an endpoint with canary strategy <endpoint_canary_update.html>`__

.. _endpoint-config:

Endpoint configuration
----------------------

* `Configuring endpoint autoscaling <endpoint_autoscaling.html>`__
* `Configuring endpoint compute resources <endpoint_resources.html>`__
* `Configuring endpoint environment variables <endpoint_env_vars.html>`__

Endpoint querying
-----------------

* `Querying an endpoint <endpoint_query.html>`__.

.. toctree::
    :hidden:
    :titlesonly:

    Creating and retrieving an endpoint <endpoint_creation>
    Model deployment <endpoint_update>
    Updating endpoints with canary <endpoint_canary_update>
    Endpoint autoscaling <endpoint_autoscaling>
    Endpoint resources <endpoint_resources>
    Endpoint environment variables <endpoint_env_vars>
    Querying an endpoint <endpoint_query>
