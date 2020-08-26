FAQs
====

- :ref:`what-parts-are-oss`
- :ref:`how-is-modeldb-different`
- :ref:`deployment-types`
- :ref:`other-questions`

.. _what-parts-are-oss:

What parts of Verta are open-source?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

All the Verta modules related to model versioning and metadata are free and open-source under ModelDB.
Find code and getting started information for ModelDB `here <https://github.com/VertaAI/modeldb>`__.

In addition, we have open-source different deployment and packaging components that are available on the
`Verta Git repository <https://github.com/VertaAI?q=&type=public>`_.

.. _how-is-modeldb-different:

How is ModelDB different from other tools like Sacred, MLFlow, WandB, Comet.ml?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Several tools are available for managing model metadata or experiment management.
ModelDB 1.0 developed at MIT pioneered the management of models that has since made its way into many tools.
However, the differentiator for ModelDB 2.0 (latest version) is the full-fledged versioning system
supported by ModelDB.
ModelDB provides a Git-like versioning system for all model ingredients including code, data, config,
and environment,  providing reproducibility  guarantees not provided by any other tools.
Read more about versioning vs. metadata `here <overview/versioning.rst>`__.

In addition to versioning, of course, ModelDB 2.0 continues to provide the metadata and experiment
management as before and is freely available as open-source.

..  _deployment-types:

Where can Verta be deployed?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Verta is a Kubernetese-based platform. As such, it can deployed on any Kubernetes cluster, whether in the cloud
or on-premise.
Verta is offered as a hosted service managed by Verta or an enterprise deployment on-premise or in a VPC.
Please send email to `support@verta.ai <mailto:support@verta.ai>`_ for more information.

..  _other-questions:

Additional Questions?
^^^^^^^^^^^^^^^^^^^^^

If your question is not covered above, please check out the `Support & Community <support.rst>`_ page for more
information about our support channels.
