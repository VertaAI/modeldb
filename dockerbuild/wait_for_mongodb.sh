#!/bin/bash
# wait-for-mongodb.sh

set -e

host="$1"
shift
cmd="$@"

# 'foo' doesn't matter here. Just needs something so that $host is interpreted
# as a hostname and not a database name.
until mongo "$host"/foo --eval "db.getCollectionNames()" > /dev/null 2>&1; do
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

exec $cmd
