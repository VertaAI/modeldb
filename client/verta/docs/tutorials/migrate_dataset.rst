Migrate legacy data versioning
==============================

|dataset versioning overhaul| This quick guide presents an overview of how the
APIs have changed, and what code changes may be necessary to continue using
dataset versioning.

Local (path) example
--------------------

There are two main changes to interacting with local datasets:

- :meth:`client.set_dataset() <verta.client.Client.set_dataset>` no longer
  accepts a ``type`` argument.
- Instead of taking a `str` for a local path,
  :meth:`dataset.create_version() <verta._dataset_versioning.dataset.Dataset.create_version>`
  now takes a :class:`~verta.dataset.Path` object that takes the `str`
  itself.

**Before:**

.. code-block:: python

    dataset = client.set_dataset(name="Census Income Local", type="local")
    version = dataset.create_version(path="census-train.csv")

**After:**

.. code-block:: python

    from verta.dataset import Path

    dataset = client.set_dataset(name="Census Income Local")  # no `type`
    version = dataset.create_version(Path("census-train.csv"))  # new argument type

S3 example
----------

There are two main changes to interacting with S3 datasets:

- :meth:`client.set_dataset() <verta.client.Client.set_dataset>` no longer
  accepts a ``type`` argument.
- Instead of taking a ``bucket_name`` and/or ``key``,
  :meth:`dataset.create_version() <verta._dataset_versioning.dataset.Dataset.create_version>`
  now takes a :class:`~verta.dataset.S3` object that takes the bucket and
  key itself, in the form ``f"s3://{bucket_name}/{key}"``.

**Before:**

.. code-block:: python

    dataset = client.set_dataset(name="Census Income S3", type="s3")
    version = dataset.create_version(
        bucket_name="verta-starter", key="census-train.csv"
    )

**After:**

.. code-block:: python

    from verta.dataset import S3

    dataset = client.set_dataset(name="Census Income S3")  # no `type`
    version = dataset.create_version(
        S3("s3://verta-starter/census-train.csv")  # one argument
    )

Prefixes are also supported—so long as they end in a slash ``'/'``:

.. code-block:: python

    version = dataset.create_version(
        S3("s3://verta-starter/models/")  # all keys that begin with "models/"
    )

Tips and tricks
---------------

Several attributes of the old ``Dataset`` and ``DatasetVersion`` classes have
been ported over and are still usable; however, most of them have been
deprecated and will raise warnings accordingly—with guidance on how to update
them.

One advantage of this new system is that you can preview the contents of your
dataset version-to-be before creating it in ModelDB:

.. code-block:: python

    from verta.dataset import S3

    S3("s3://verta-starter/census-train.csv")
    # S3 Version
    #     s3://verta-starter/census-train.csv
    #         3271573 bytes
    #         last modified: 2019-05-24 07:25:26
    #         MD5 checksum: 64af2ff44dd04acceb277d024939b619

The content object can also be recovered from a dataset version:

.. code-block:: python

    version = dataset.create_version(S3("s3://verta-starter/census-train.csv"))

    version.get_content()
    # S3 Version
    #     s3://verta-starter/census-train.csv
    #         3271573 bytes
    #         last modified: 2019-05-24 07:25:26
    #         MD5 checksum: 64af2ff44dd04acceb277d024939b619

In addition, because a dataset is no longer restricted to a particular type,
its versions can be different types themselves:

.. code-block:: python

    from verta.dataset import Path, S3

    dataset = client.create_dataset()
    dataset.create_version(Path("census-train.csv"))
    dataset.create_version(S3("s3://verta-starter/census-train.csv"))

For the complete functionality, please see the updated
`API reference <../api/api/dataset.html>`__!
