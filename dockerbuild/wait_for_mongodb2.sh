#!/bin/bash
# wait-for-mongodb.sh

# Usage: sh /modeldb/dockerbuild/wait_for_mongodb.sh 0.10.0 mongo

set -e

thrift_version="$1"
host="$2"
echo "$host"

# 'foo' doesn't matter here. Just needs something so that $host is interpreted
# as a hostname and not a database name.
until mongo "$host"/foo --eval "db.getCollectionNames()" > /dev/null 2>&1; do
    >&2 echo "MongoDB is unavailable - sleeping"
    sleep 1
done

>&2 echo "MongoDB is up - executing command"

# Substitute the desired MongoDB hostname in the built project's conf file.
# before="$PWD"
# cd /modeldb/server
# cp src/main/resources/reference-docker.conf target/classes/reference.conf
# sed -i "s/MONGODB_HOST/$host/" target/classes/reference.conf
# cd "$before"

# set up sqlite
if [ ! -f /db/modeldb.db ]; then
    touch /db/some
    cat /sqlite_scripts/createDb.sql | sqlite3 /db/modeldb.db
    cat /sqlite_scripts/createDb.sql | sqlite3 /db/modeldb_test.db
fi

java -cp /modeldb/server/target/uber-modeldb.jar edu.mit.csail.db.ml.main.Main