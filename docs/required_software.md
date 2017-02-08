# Downloading the Dependencies
## Requirements
ModelDB requires Linux or MacOS.

Note that the version number in parentheses is the recommended version that has been tested.

**(\*\*) = Must have exact version.**

*Server*
* [SQLite](http://sqlite.org/) (3.15.1): To store the models
* [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (1.8): To run the server
* [Apache Thrift](http://thrift.apache.org/) (0.9.3)\*\*: To interface with clients and frontend
* [Maven](http://maven.apache.org/download.cgi) (3.3.9): To build the project

*If using the Python (scikit-learn) client*
* [Python 2.7](https://www.continuum.io/downloads): To run the Python client

*If using the Scala (spark.ml) client* 
* [SBT](http://www.scala-sbt.org/) (0.13.12): To build scala client
* [Apache Spark](https://spark.apache.org/downloads.html) (2.0.0 - NOT LATEST)\*\*: To run workflows

*Frontend*
* [Nodejs](https://nodejs.org/en/): To browse ModelDB data on the web

For Linux, you can also refer to [this script](install_on_linux.sh). 
