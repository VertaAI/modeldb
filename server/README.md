# Introduction

ModelDB is a system for storing machine learning operations. This repository 
houses the ModelDB server, which is responsible for exposing a Thrift API so 
that clients can store machine learning operations, like training a model, in
a database.

# Usage

Make sure you've installed SQLite3. 

Create the SQLite database and tables for ModelDB using:

```
cd codegen && ./gen_sqlite.sh && cd ..
```

To launch the server, make sure you have installed Maven 3.

```
./start_server.sh
```

Now, you can use one of the ModelDB clients, like the 
[Spark client](https://github.com/mitdbg/spark-modeldb-client) or the 
[Scikit-learn client](https://github.com/mitdbg/sklearn-modeldb-client) to then
store entries in ModelDB.

# Tables

Here are the [schemas](https://github.com/mitdbg/modeldb/blob/master/codegen/sqlite/createDb.sql)
for the tables created by ModelDB.

# Configuration
Edit the [configuration file](https://github.com/mitdbg/modeldb/blob/master/server/src/main/resources/reference.conf) to your liking.

Currently, only SQLite is supported, so you cannot change the database type.

If you have your SQLite datbase stored at `/path/to/dbFile.db`, then adjust
the `jdbcUrl` field to be: `"jdbc:sqlite:/path/to/dbFile.db"`
