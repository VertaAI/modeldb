#!/bin/bash
# wait-for-mongodb.sh

set -e

host="$1"
shift
cmd="$@"

until mongo mongo/foo --eval "db.getCollectionNames()" > /dev/null 2>&1; do
    >&2 echo "MongoDB is unavailable - sleeping"
    sleep 1
done

>&2 echo "MongoDB is up - executing command"

pwd
echo $cmd

exec $cmd
