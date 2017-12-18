=======
modeldb
=======

A python client for working with `ModelDB machine learning management system <http://modeldb.csail.mit.edu>`_.

This library makes it easy for users of the `ModelDB ML management system <http://modeldb.csail.mit.edu>`_ to automatically catalog models built with `scikit-learn <scikit-learn.org>`_.


ModelDB is an end-to-end system for managing machine learning models. It ingests models and associated metadata as models are being trained, stores model data in a structured format, and surfaces it through a web-frontend for rich querying. ModelDB runs on Python 2.X and 3.X and can be used with any ML environment via the ModelDB Light API.


Quick start
===========

Install
-------

You can install it using ``pip`` directly from PyPI::

    pip install modeldb


Custom Configuration
--------------------

Once installed, you can create a custom syncing scheme setup by typing::

    python -m modeldb create_config

Unless an alternative syncing scheme is specialized, modeldb will use the packaged `syncer.json` defaults.


Use
---

This library requires a connection to a ModelDB server to work. You can see the `getting started docs here <https://github.com/mitdbg/modeldb/blob/master/docs/getting_started/scikit_learn.md>`_.

Additional documentation on the `light_api <light_api.md>`_ and `scikit-learn client <scikit_learn.md>`_ is also available.
