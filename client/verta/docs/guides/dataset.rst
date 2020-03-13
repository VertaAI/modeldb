Logging a Data Version
======================

We'll walk briefly through the concept of data versioning, and how it can be handled by the Verta
client!

Data versioning is a key part of how Verta versions models; as a result, Verta supports datasets
as first-class citizens of the system. This means that datasets (and dataset versions) can be created
independent of models and experiments, reused across multiple models, and queried independently.


Context
-------

In Verta, a dataset is conceptualized as a collection of *dataset versions* where each version contains
metadata about the actual data—a new version of a dataset is created *only* if the system detects a
change in the associated metadata.

The metadata stored for a dataset version depends on the type of dataset (e.g. RDBMS, file system,
blob store). For instance, if a dataset is of type S3, then the version information consists of
metadata about every object in a specified S3 bucket, such as each object's size, date modified,
and checksum.

The Verta client library provides a variety of APIs to perform operations on datasets and dataset
versions. Once dataset versions have been created, any experiment run can be associated with one
or more dataset versions using ``run.log_dataset_version()`` as shown in this guide.


Setup
-----

This guide assumes a basic familiarity with Verta's client interface.

Imagine we have some CSV files stored on `Amazon S3 <https://aws.amazon.com/s3/>`_ that we would
like to associate with our workflows. We'd like to keep track of which files we're accessing, and
when they are accessed.

First, we'll need to install ``boto3``, the official Python library for Amazon Web Services:

.. code-block:: console

    pip install boto3

Installing the Verta client did not install ``boto3`` automatically since it's not required for
core functionality, but it is required for data versioning with S3.

Don't worry—Verta doesn't download or store the actual S3 object; instead, we take in just enough
information for you to later identify the snapshot of data that was used.

After installation, make sure AWS credentials are set up in your local environment, following their
`official instructions <https://pypi.org/project/boto3/>`_.


Version Logging
---------------

Now, in Python, we'll instantiate the :class:`~verta.client.Client`:

.. code-block:: python

    from verta import Client
    client = Client(host, email, dev_key)
    # connection successfully established

The client can be used to create an :class:`~verta._dataset.S3Dataset`:

.. code-block:: python

    dataset = client.set_dataset(name="Important Data",
                                 type="s3")

A *Dataset* is a collection of related *DatasetVersion*\ s, and we'll need to create an
:class:`~verta._dataset.S3DatasetVersion` to be logged:

.. code-block:: python

    dataset_version = dataset.create_version(bucket_name="datasets",
                                             key="important-data.csv")

Note here that ``key`` is optional; if omitted, we would instead be tracking the entire bucket.

As we track our data science workflow, we can log this dataset version:

.. code-block:: python

    run.log_dataset_version("training_data", dataset_version)


Version Viewing
---------------

Once a dataset version is logged, it can be viewed in the Verta Web App.

You'll find the dataset version in the **Datasets** section of the ExperimentRun page:

.. image:: /_static/images/dataset-version-section.png

Clicking on *training_data* will direct you to the DatasetVersion page:

.. image:: /_static/images/dataset-version-popup.png

And there, you'll find information about your dataset version:

.. image:: /_static/images/dataset-version-page.png
