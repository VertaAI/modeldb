Verta
=====

The Complete MLOps Platform
---------------------------

Verta is a complete MLOps (ML Operations) platform focused on operationalization of ML 
models, i.e., integrating ML development and delivery into regular software in a way 
that allows data scientists to continue focus on machine learning and data science, 
while providing ML and DevOps Engineers the means to safely and reliably integrate ML into the
broader software ecosystem in any organization.


As shown below, the full ML lifecycle consists of three components: the data preparation
loop (including ETL, Data Cleaning), the model development loop (including training, feature pre-processing,
model validation), and model operationalization (including packaging, release, monitoring and operations). 
Verta comes in during model development and provides model versioning capabilities via ModelDB.
ModelDB then serves as the connection between the development and operationalization phases.
During the operationalization phase, Verta provides modular components for model deployment, release,
monitoring, and maintenance.

.. image:: /_static/images/ml-lifecycle-1.png
    :width: 100%

Verta is an open-core platform; i.e., the platform is based on core open-source technology
developed by the Verta team that is freely available. Find more information about our
open-source technology `here <https://verta.ai/open-source>`_ and in subsequent
sections.

Verta provides MLOps functionality in three key areas: **model versioning and metadata**, 
**model deployment and release**, and **real-time model monitoring**.

===========================
Model Versioning & Metadata
===========================

The first step to enable operationalization of ML models is to make them reproducible and 
associate governance data with them. Verta's open-source `ModelDB <https://github.com/VertaAI/modeldb>`_ 
component is the only system to provide full model versioning and reproducibility along
with a rich metadata system.

Head over to :doc:`overview/versioning` to learn more about to use ModelDB
in your ML workflows.

==========================
Model Deployment & Release
==========================

The most challenging and yet crucial operation in operationalization of models is model 
deployment and release.
Due to the diversity of ML frameworks and libraries, along with the  disparate systems 
for ML development vs. software delivery systems, it takes many months to release models
into products.

One of Verta's key innovations is our open-core model deployment and release system that 
works seamlessly with models built in over a dozen frameworks and languages, and integrates
with state-of-the-art DevOps and software delivery systems.

Head over to :doc:`overview/deployment` to deploy your models. 

==========================
Real-time Model Monitoring
==========================

Once a model is deployed, close monitoring is required, both at the systems level  (e.g. CPU,
memory, network) as well as the feature and data level to ensure that the model is performing 
at the highest levels and rapidly remedy production incidents.

Verta's model monitoring provides both system and data monitoring to ensure real-time model
health. Head over to :doc:`overview/monitoring` to learn more about how to monitor
your live models.


Getting Started with Verta
^^^^^^^^^^^^^^^^^^^^^^^^^^

Ready to get started? We recommend the following:

- :doc:`quickstart`
- :doc:`examples`

.. toctree::
    :hidden:
    :titlesonly:

    Versioning & Metadata <overview/versioning>
    Deployment & Release <overview/deployment>
    Model Monitoring <overview/monitoring>
    ModelDB <overview/modeldb>