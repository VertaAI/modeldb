.. versioning-and-metadata:

Versioning & Metadata
=====================

Verta is singular in ML Infrastructure systems in that in provides both *versioning* and *metadata* management.
While versioning allows safety and reproducibility for models, metadata provides ability for model
governance, compliance, and visibility.

In this page, learn about:

- :ref:`concepts`
- :ref:`modeldb`

.. note::
    Looking for a tutorial? Head on to :doc:`../tutorials/tutorials` for a step-by-step walkthrough of Verta's
    versioning and metadata capabilities.

.. _concepts:

========
Concepts
========

What is Versioning?
-------------------

Versioning is the ability to track changes to an ML model over time and uniquely identify each state of an
ML model by an ID so that one can navigate between different model states.
In Verta, we version the *constituent elements* used to create a trained model (or inputs to a training  process) as first-class entities
in the system; specifically, we version the **code, data, configuration, and the compute environment** for a model.
The output of a training process such as model weights or checkpoints are not part of the model version
directly; instead these outputs are tracked via Artifacts and Metadata.

This distinction is shown in the picture below.

.. image:: /_static/images/versioning-overview-1.png
    :width: 60%


What is metadata?
-----------------

Metadata is extra (or "meta") data about any of entities in the system such as Projects, Experiments,
ExperimentRuns, and Models.
Examples of metadata include:

+------------------------+------------+----------+----------+
|Entity                  | Examples of metadata             |
|                        |                                  |
+========================+============+==========+==========+
| Project                | Tags, owner, date created        |
+------------------------+------------+----------+----------+
| Experiment             | Tags, owner, date created        |
+------------------------+------------+----------+----------+
| ExperimentRun          | Metrics, AUC curves, tags, owner |
+------------------------+------------+----------+----------+
| Model                  | Name, tags, lifecycle stage      |
+------------------------+------------+----------+----------+

How is Metadata different from Versioning?
------------------------------------------

Metadata, however extensive, does not enable you to go forward or backward in time to a specific
state of a model.
For instance, with metadata alone, you cannot go back to the exact state when a model was created
three months back.

On the other hand, versioning is restrictive in what information is captured in a version.
For instance, versions do not include extraneous artifacts like documentations and reports that are
essential for data science activities.
Only metadata can provide such information.
As a result, the combination of versioning and metadata together is extremely powerful.


What is an Artifact?
--------------------

An artifact is any binary or blob-like information. This may include the weights of a model, model
checkpoints, charts produced during training, etc.
In Verta, artifacts can be associated with a variety of entities including Projects and ExperimentRuns
(most common).


.. _modeldb:

================================
Versioning and Metadata in Verta
================================

Verta's ModelDB system provides model versioning as well as metadata capabilities for machine learning
models.
In order words, ModelDB enables data scientists to version their models and make them reproducible and
enable them to associate rich metadata with these models.

Head over to the :doc:`modeldb` page for details about ModelDB capabilities.
