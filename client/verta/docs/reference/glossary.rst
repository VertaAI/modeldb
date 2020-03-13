Glossary
========

Organizational Hierarchy
------------------------
Verta organizes data science work into a three-tiered hierarchy.

Project
^^^^^^^
A **Project** is the modeling task at hand, such as "Image Recommendation" or "Sentiment Analysis
of Amazon Reviews".

Experiment
^^^^^^^^^^
An **Experiment** is the configurable strategy being used to accomplish the Project, such as
"Logistic Regrssion" or "Convolutional Neural Network with tf-idf".

Experiment Run
^^^^^^^^^^^^^^
An **Experiment Run** roughly corresponds to one execution of a Python script or Jupyter notebook,
and represents a particular configuration of an Experiment. In most cases, an Experiment Run
produces a single model as its end result.

Artifacts
---------
An **artifact** is any object that is the result of an Experiment Run. Examples include:
* serialized instances of a model
* images produced during the training process
