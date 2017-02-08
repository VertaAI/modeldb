# Downloading the Dependencies
## Requirements
ModelDB requires Linux or MacOS.

ModelDB depends on the following:
(The version number in parentheses is a recommended version that has been tested and works)

*Server*
* [SQLite](http://sqlite.org/) (3.15.1): To store the models
* [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (1.8): To run the server
* [Apache Thrift](http://thrift.apache.org/) (0.9.3): To interface with the different clients *ModelDB is incompatible with other versions of Thrift*
* [Maven](http://maven.apache.org/download.cgi) (3.3.9): To build the project

*Client (Python)*
* [Anaconda for Python 2.7](https://www.continuum.io/downloads) (4.2.0): To run the Python client. Standard Python 2.7 also works, 
but then various scientific libraries will need to be downloaded seperately.
* [Apache Thrift](http://thrift.apache.org/) (0.9.3) (latest): To interface with the server

*Client (Scala)* 
* [SBT](http://www.scala-sbt.org/) (0.13.12): In order to build the Scala project (comes with Scala)
* [Apache Thrift](http://thrift.apache.org/) (0.9.3): To interface with the server
* [Apache Spark](https://spark.apache.org/downloads.html) (2.0.0 - NOT LATEST): To train and run machine learning models

*Other Useful Tools*
* [Git](https://git-scm.com/) (latest): To clone the project
* [IntelliJ IDE](https://www.jetbrains.com/idea/#chooseYourEdition) (latest): To write Scala and Java code

Once you've downloaded all the dependencies, continue onto:

[Setting Up the Server and Client](RunningTheClientAndServer.md)

If you would like more detailed installation instructions check out these:

For [Linux](InstallOnLinux.sh) you can use this .sh file. You can also look at the commands to see which commands to run to install each dependency.
