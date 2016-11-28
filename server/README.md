# Introduction

The ModelDB server is responsible for storing all the ModelDB data and exposing
a query interface for this data. The ModelDB server exposes a thrift API for storing
data to and querying data from the ModelDB.

# Setup

ModelDB Server currently uses SQLite3 for storing data. So make sure you've installed SQLite3 (see [dependencies] (https://github.com/mitdbg/modeldb/blob/master/docs/RequiredSoftware.md)). 

From the server directory, create the SQLite database and tables for ModelDB using:

```
cd codegen && ./gen_sqlite.sh && cd ..
```

To launch the server, make sure you have installed Maven 3.

```
./start_server.sh
```

# Tests

You can run server tests with `mvn test`

# Configuration
Edit the server [configuration file](https://github.com/mitdbg/modeldb/blob/master/server/src/main/resources/reference.conf) to your liking.

Currently, only SQLite is supported, so you cannot change the database type.

If you have your SQLite datbase stored at `/path/to/dbFile.db`, then adjust
the `jdbcUrl` field to be: `"jdbc:sqlite:/path/to/dbFile.db"`
