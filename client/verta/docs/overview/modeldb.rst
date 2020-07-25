******************************************************
ModelDB: Open-Source Model Versioning and Metadata
******************************************************

ModelDB is the fully open-source model versioning, metadata and experiment management component of the Verta platform.

- :ref:`modeldb-use-cases`
- :ref:`modeldb-features`
- :ref:`modeldb-repository`
- :ref:`modeldb-history`

.. _modeldb-use-cases:

Use Cases
=========

1. Make your ML models reproducible
------------------------------------

ML models currently have a major problem with reproducibility that causes weeks to be spent on audit 
inquiries in regulated industries and challenges fixing production issues with models.
ModelDB provides the ability to version every change to a model and make a model fully reproducible.

By versioning the *constituent elements* required for the creation of a model, ModelDB can enable 
reproducibility of all models and drastically reduce the time required to answer production incidents as
well as regulatory inquiries.

2. Manage your ML experiments
------------------------------

Building an ML model involves a lot of trial and error and the building of hundreds to thousands of models.
Each of these experiments in model building are essential to the development of the final model and for
analyzing and debugging the performance of different models.

Use ModelDB to version the models built in a project and manage the experiments via intuitive dashboards,
reporting, and model analysis functionality.


3. Increase Data Science and ML Visibility Across the organization
--------------------------------------------------------------------

As more models are built across a team or an organization, there are productivity benefits to be obtained by sharing
ML know-how and models across team members.
Towards this, a central model repository such as one provided by ModelDB can enable model sharing, 
collaboration, and reuse.

4. Managing the Model Lifecycle
----------------------------------------------------------------

Once models are built, they go through different steps in their lifecycle as they are packaged, deployed,
released, and eventually retired.
Using ModelDB as a central source of truth on model status can enable teams to get a central view into the
stage of a model within its lifecycle.


.. _modeldb-features:

Features
========

Model Versioning
----------------

* Complete and flexible model versioning and reproducibility
* Data Versioning across file systems (e.g., HDFS), blob stores (e.g., S3, GCP), and databases (e.g., BigQuery, Postgres, MySQL)
* Git-like operations on any model
* Central model repository functionality

Model Lifecycle Management and Governance
------------------------------------------

* Flexible metadata logging including metrics, artifacts, tags and user information
* ML Experiment management
* Model Governance and management
* Beautiful dashboards for model performance and reporting

Enterprise Readiness
---------------------

* Battle-tested in production environments
* Model sharing, collaboration, and access control
* User management and plugins into enterprise authentication systems

Technical Features
------------------

* Clients in Python and Scala
* Integration into state-of-the-art frameworks like Tensorflow and PyTorch
* Kubernetes compatible
* Pluggable storage and artifact systems

.. _modeldb-repository:

Repository
==========

The ModelDB repository is located `here <https://github.com/VertaAI/modeldb>`_. Please clone, star, and contribute!

.. _modeldb-history:

History
=======

ModelDB 1.0 started in 2016 as a research project in the `Database Group <http://dsg.csail.mit.edu>`_ at `MIT CSAIL <http://csail.mit.edu>`_.
At the time, ModelDB was focused on model metadata management and pioneered the approach that is now used in many research and commercial systems.
Since then, ModelDB evovled to be the first model versioning system to provide model reproducibility and along with metadata management.
ModelDB 2.0 is maintained by `Verta.ai <https://verta.ai>`_.
