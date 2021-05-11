Tutorials
=========

Check out these tutorials for detailed step-by-step instructions on performing different MLOps
functions in Verta.

* `Packaging models <packaging/packaging.html>`_ for use in different applications.

ModelDB (versioning and metadata)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* `Versioning model ingredients <ingredients.html>`_, which allows you to have Git-like
  tracking for the components of models
* `Versioning a model and logging metadata <workflow.html>`_
* `Versioning data <dataset.html>`_
* `Metadata querying <querying.html>`_
* `Handling merge conflicts <merge.html>`_


Deployment and release
^^^^^^^^^^^^^^^^^^^^^^

* `Creating and retrieving an endpoint <deployment/endpoint_creation.html>`__
* `Updating an endpoint <deployment/endpoint_update.html>`__
* `Updating an endpoint with canary strategy <deployment/endpoint_canary_update.html>`__
* `Configuring endpoint autoscaling <deployment/endpoint_autoscaling.html>`__
* `Configuring endpoint compute resources <deployment/endpoint_resources.html>`__
* `Configuring endpoint environment variables <deployment/endpoint_env_vars.html>`__
* `Configuring endpoint using config file <deployment/endpoint_update_config.html>`__
* `Querying an endpoint <deployment/endpoint_query.html>`__

Real-time model monitoring
^^^^^^^^^^^^^^^^^^^^^^^^^^

.. note::
    This functionality is only available in Verta Enterprise.
    Please email `support@verta.ai <mailto:support@verta.ai>`_ for your enterprise login.

Core Verta functionality
^^^^^^^^^^^^^^^^^^^^^^^^

* `Using the Verta web app <webapp.html>`_
* `Workspaces in Verta <workspaces.html>`_
* `Using a client config file <config.html>`_

.. toctree::
    :hidden:
    :titlesonly:

    Model ingredient versioning <ingredients>
    Model versioning <workflow>
    Data versioning <dataset>
    migrate_dataset
    Metadata querying <querying>
    Merge conflicts <merge>
    Packaging <packaging/packaging>
    Deployment <deployment/deployment>
    Organizations <organizations/organizations>
    Web app <webapp>
    sharing
    Workspaces <workspaces>
    custom_users
    Client config file <config>
