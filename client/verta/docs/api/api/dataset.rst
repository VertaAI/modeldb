Data Versioning
===============


Dataset Creation and Retrieval
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. automethod:: verta.client.Client.set_dataset
    :noindex:
.. automethod:: verta.client.Client.find_datasets
    :noindex:


Dataset Version Creation
^^^^^^^^^^^^^^^^^^^^^^^^

.. automethod:: verta._dataset.LocalDataset.create_version
    :noindex:
.. automethod:: verta._dataset.S3Dataset.create_version
    :noindex:
.. automethod:: verta._dataset.AtlasHiveDataset.create_version
    :noindex:
.. automethod:: verta._dataset.BigQueryDataset.create_version
    :noindex:


Dataset Version Logging and Retrieval
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. automethod:: verta.client.ExperimentRun.log_dataset_version
    :noindex:
.. automethod:: verta.client.ExperimentRun.get_dataset_version
    :noindex:
.. automethod:: verta.client.Client.get_dataset_version
    :noindex:
