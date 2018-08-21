#!/bin/bash
# wait-for-mongodb.sh

# Usage: sh /modeldb/dockerbuild/wait_for_mongodb.sh 0.10.0 mongo

set -e

thrift_version="$1"
host="$2"

export PASSWORD=$(< /mongo/mongodb-password)

until pgrep mongod > /dev/null 2>&1; do
    >&2 echo "MongoDB is unavailable - sleeping"
    sleep 1
done
>&2 echo "MongoDB is up - executing command"

# Substitute the desired MongoDB hostname in the built project's conf file.
before="$PWD"
cd /modeldb/server
cp src/main/resources/reference-docker.conf target/classes/reference.conf
sed -i "s/MONGODB_HOST/$host/" target/classes/reference.conf
cd "$before"

exec mvn exec:java -Dexec.mainClass='edu.mit.csail.db.ml.main.Main' -Dthrift_version=$thrift_version
