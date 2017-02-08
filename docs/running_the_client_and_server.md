# Setting up the Client and Server

## In Short
*from the modeldb/ directory*
### Server

`cd server`  
`cd codegen`  
`./gen_sqlite.sh`  
`cd ..`    
`./start_server.sh`

### Client

`cd client/scala/libs/spark.ml` 
`./build_client.sh`

The JAR file is then in:  
`target/scala-2.11/ml.jar`

## The Server, Step by Step
The server is in the `modeldb/server/` directory. 

**From here on out,
all commands will assume that you are in the `/server` directory.**

`cd server`

### Environment Variables

Make sure the following variables are set:
* `PATH`: Should include bin folders of SQLite, Maven, Java, Anaconda
* `JAVA_HOME`: Should be set to the main directory (not bin) of your jdk

### Setting up the SQLite tables
Navigate to the `codegen` directory, and run the sh file `gen_sqlite.sh`

`cd codegen`
`./gen_sqlite.sh`
`cd ..`

This will produce the tables necessary to run the modeldb server.

### Launching the Server
Now, launch the server using the `start_server.sh` script. You need Maven installed
to do so.

`./start_server.sh`

### Testing the Server
You can now test the server with a sample client by running:

`mvn test`

## The Scala Client
The scala client has a fairly large directory structure. The following commands
will assume that you are in the `spark.ml` directory.

`cd client/scala/libs/spark.ml`

### Environment Variables
You will need the following environment variables set:

* `PATH`: Must include the bin directory of *sbt* (Scala build tool), Anaconda, and Apache Spark.

### Assembling the JAR File
Assemble the JAR file using the executable. Internally, this assumes SBT and Anaconda are in your PATH.

*From client/scala/libs/spark.ml/*

`./build_client.sh`

This will create a jar:

`target/scala-2.11/ml.jar`

You can then use this jar in your projects.

### Testing the JAR with Sample Projects

ModelDB also includes a few sample projects for you to run. Let's run one 
to make sure the target compiled correctly.

First, download the adult data set from [http://archive.ics.uci.edu/ml/datasets/Adult](http://archive.ics.uci.edu/ml/datasets/Adult)

Then, from the spark.ml directory, run

`spark-submit --master local[*] --class "edu.mit.csail.db.ml.modeldb.sample.CompareModelsSample" target/scala-2.11/ml.jar <path_to_adult.data>`





