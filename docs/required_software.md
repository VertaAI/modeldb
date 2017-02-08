# Downloading the Dependencies
## Requirements
ModelDB requires Linux or MacOS.

ModelDB depends on the following packages/libraries:

Note that the version number in parentheses is a recommended version that has been tested. 
**Thrift**, **Spark**, and **Python** dependencies in particular are sensitive to versions.

*Server*
* [SQLite](http://sqlite.org/) (3.15.1): To store the models
* [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (1.8): To run the server
* [Apache Thrift](http://thrift.apache.org/) (0.9.3): To interface with the different clients
* [Maven](http://maven.apache.org/download.cgi) (3.3.9): To build the project

*If using the Python (scikit-learn) client*
* [Python 2.7](https://www.continuum.io/downloads): To run the Python client.
* [Apache Thrift](http://thrift.apache.org/) (0.9.3): To interface with the server

*If using the Scala (spark.ml) client* 
* [SBT](http://www.scala-sbt.org/) (0.13.12): In order to build the Scala project (comes with Scala)
* [Apache Thrift](http://thrift.apache.org/) (0.9.3): To interface with the server
* [Apache Spark](https://spark.apache.org/downloads.html) (2.0.0 - NOT LATEST): To train and run machine learning models

For Linux, you can also use [this script](install_on_linux.sh). 
