Core APIs
=========

The core Verta APIs are used to establish connections between the Verta clients and Verta servers 
as well as manage authentication and access control.
Two types of core constructs are essential to note when using Verta: the Verta `Client` and 
the Verta `Organizational Hierarchy` described below.

Verta Client
------------

The Verta `Client` object used to manage connections between the Verta clients and servers. 
The client object also manages authentication information.

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
"Logistic Regression" or "Convolutional Neural Network with tf-idf".

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

.. toctree::
    :hidden:
    :titlesonly:

    Client <api/client>
    Project <api/project>
    Experiment <api/experiment>
    ExperimentRun (Core) <api/experimentrun_basic>
    ExperimentRuns <api/experimentruns>
    
