#!/bin/bash

if [ ! -f /db/modeldb.db ]; then
    cat /sqlite_scripts/createDb.sql | sqlite3 /db/modeldb.db
    cat /sqlite_scripts/createDb.sql | sqlite3 /db/modeldb_test.db
    echo "created DB"
fi
