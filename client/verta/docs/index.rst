Verta: The Complete MLOps Platform
==================================

Verta is a complete MLOps (ML Operations) platform focused on operationalization of ML 
models, i.e., integrating ML development and delivery into regular software in a way 
that allows data scientists to continue focus on machine learning and data science, 
while providing ML and DevOps Engineers to safely and reliably integrate ML into the
broader software ecosystem in any organization.


As shown below, the full ML lifecycle consists of three components: the data preparation
stage, the model development, and model operationalization. 
Verta comes in during model development and provides model versioning that serves as the
connection between the development and operationalization phases.
During operationalization, Verta provides modular components for model deployment, release,
monitoring, and maintenance.

.. image:: /_static/images/ml-lifecycle-1.png
    :width: 100%

(Learn more about the full ML lifecycle `here (fix this) <https://app.verta.ai/profile>`__.)

Verta is an open-core platform; i.e., the platform is based on core open-source technology
developed by the Verta team that is freely available. Find more information about our
open-source technology `here (fix this) <https://app.verta.ai/profile>`__ and in subsequent
sections.

We categorize Verta functionality into three key areas as below: model versioning and metadata, 
model deployment and release, and real-time model monitoring.

===========================
Model Versioning & Metadata
===========================

The first step to enable operationalization of ML models is to make them reproducible and 
associate governance data with them. Verta's open-source `ModelDB (fix this) <link>`__ 
component is the only system to provide full model versioning and reproducibility along
with a rich metadata system.

Head over to `Versioning & Metadata (fix this) <link>`__ to learn more about to use ModelDB
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

Head over to `Deployment and Release (fix this) <linnk>`__ to deploy your models. 

==========================
Real-time Model Monitoring
==========================

Once a model is deployed, close monitoring is required, both at the systems level  (e.g. CPU,
memory, network) as well as the feature and data level to ensure that the model is performing 
at the highest levels and rapidly remedy production incidents.

Verta's model monitoring provides both system and data monitoring to ensure real-time model
health. Head over to `Model Monitoring (fix this) <link>`__ to learn more about how to monitor
your live models.


Getting Started
^^^^^^^^^^^^^^^

Install the Verta package into your ML environment and get started with Verta.

- `Quickstart (fix this) <quickstart.html>`__
- Version PyTorch  models
- Version TF models

Examples
^^^^^^^^

Head over to `Examples  & Tutorials(fix this) <examples.html>`__ to see how Verta 
integrated into end-to-end examples. Here are a few we like:

- Model Versioning for Compliance
- Model Deployment using Jenkins and Prometheus (from Strata San Jose, March 2020)


.. toctree::
    :hidden:
    :titlesonly:

    Verta <verta>
    Quickstart <quickstart>
    Examples & Tutorials <examples>
    Support & Community <support>
    Resources <learn>
    FAQs <faqs>
    Release Notes <change_log>
    API Reference <api_reference>
