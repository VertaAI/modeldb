# Downloading the Dependencies
## Requirements
ModelDB's architecture consists of code that is run on the client side, in the 
machine learning models, and a server side that runs a database to store the
models. In order to run properly, you must use Linux or MacOS.

ModelDB depends on the following:
(The version number in parentheses is a recommended version that has been tested and works)

*Server*
* [SQLite][] (3.13.0): To store the models
* [Java][] (1.8): To run the server
* [Apache Thrift][] (0.9.3): To interface with different languages
* [Maven][] (3.3.9): To build the project

[SQLite]: (http://sqlite.org/)
[Java]: (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[Apache Thrift]: (http://thrift.apache.org/)
[Maven]: (http://maven.apache.org/download.cgi)

*Client (Python)*
* [Anaconda for Python 2.7][] (4.2.0): To run the python client. Standard python 2.7 also works, 
but then various scientific libraries will need to be downloaded seperately.
* [Apache Thrift][] (0.9.3) (latest): To interface with different languages

*Client (Scala)* 
* [SBT][] (0.13.12): In order to build the scala project (comes with Scala)
* [Apache Thrift][] (0.9.3): To interface with different languages
* [Apache Spark][] (2.0.0 - NOT LATEST): To train and run machine learning models

[Anaconda for Python 2.7]: (https://www.continuum.io/downloads)
[Scala]: (http://www.scala-lang.org/download/)
[SBT]: (http://www.scala-sbt.org/)
[Apache Spark]: (https://spark.apache.org/downloads.html)

*Other Useful Tools*
* [Git][] (latest): To clone the project
* [IntelliJ IDE][] (latest): To write Scala and Java code

[Git]: (https://git-scm.com/)
[IntelliJ IDE]: (https://www.jetbrains.com/idea/#chooseYourEdition)


