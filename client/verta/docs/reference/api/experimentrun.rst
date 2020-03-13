.. currentmodule:: verta.client

ExperimentRun
=============

.. autoclass:: verta.client.ExperimentRun

Functionality
-------------

- `Miscellaneous`_

- `Basic Metadata`_

  - `Attributes`_
  - `Hyperparameters`_
  - `Metrics`_
  - `Observations`_
  - `Tags`_

- `Artifacts`_

  - `General`_
  - `Datasets`_
  - `Images`_

- `Advanced Uses`_

  - `Code Versioning`_
  - `Data Versioning`_
  - `Deployment`_
  - `Versioning`_

Miscellaneous
-------------

.. automethod:: ExperimentRun.clone

Basic Metadata
--------------

Attributes
^^^^^^^^^^
|attributes description|

.. automethod:: ExperimentRun.log_attribute
.. automethod:: ExperimentRun.log_attributes
.. automethod:: ExperimentRun.get_attribute
.. automethod:: ExperimentRun.get_attributes

Hyperparameters
^^^^^^^^^^^^^^^
|hyperparameters description|

.. automethod:: ExperimentRun.log_hyperparameter
.. automethod:: ExperimentRun.log_hyperparameters
.. automethod:: ExperimentRun.get_hyperparameter
.. automethod:: ExperimentRun.get_hyperparameters

Metrics
^^^^^^^
|metrics description|

.. automethod:: ExperimentRun.log_metric
.. automethod:: ExperimentRun.log_metrics
.. automethod:: ExperimentRun.get_metric
.. automethod:: ExperimentRun.get_metrics

Observations
^^^^^^^^^^^^
|observations description|

.. automethod:: ExperimentRun.log_observation
.. automethod:: ExperimentRun.get_observation
.. automethod:: ExperimentRun.get_observations

Tags
^^^^
|tags description|

.. automethod:: ExperimentRun.log_tag
.. automethod:: ExperimentRun.log_tags
.. automethod:: ExperimentRun.get_tags

Artifacts
---------

General
^^^^^^^
.. automethod:: ExperimentRun.log_artifact
.. automethod:: ExperimentRun.log_artifact_path
.. automethod:: ExperimentRun.get_artifact

Datasets
^^^^^^^^
.. automethod:: ExperimentRun.log_dataset
.. automethod:: ExperimentRun.log_dataset_path
.. automethod:: ExperimentRun.get_dataset

Images
^^^^^^
.. automethod:: ExperimentRun.log_image
.. automethod:: ExperimentRun.log_image_path
.. automethod:: ExperimentRun.get_image

Advanced Uses
-------------

Code Versioning
^^^^^^^^^^^^^^^
.. automethod:: ExperimentRun.log_code
.. automethod:: ExperimentRun.get_code

Data Versioning
^^^^^^^^^^^^^^^
.. automethod:: ExperimentRun.log_dataset_version
.. automethod:: ExperimentRun.get_dataset_version

.. _experiment-run-deployment:

Deployment
^^^^^^^^^^
Logging
"""""""
.. automethod:: ExperimentRun.log_model
.. automethod:: ExperimentRun.get_model
.. automethod:: ExperimentRun.log_requirements
.. automethod:: ExperimentRun.log_setup_script
.. automethod:: ExperimentRun.log_training_data
.. automethod:: ExperimentRun.fetch_artifacts

Deploying
"""""""""
.. automethod:: ExperimentRun.get_deployment_status
.. automethod:: ExperimentRun.deploy
.. automethod:: ExperimentRun.undeploy
.. automethod:: ExperimentRun.get_deployed_model

Deprecated
""""""""""
.. automethod:: ExperimentRun.log_model_for_deployment
.. automethod:: ExperimentRun.log_modules

.. _experiment-run-versioning:

Versioning
^^^^^^^^^^
.. automethod:: ExperimentRun.log_commit
.. automethod:: ExperimentRun.get_commit
