#!/bin/bash
# wait-for-backend.sh

# This script should be run from /modeldb/frontend so that node has access to
# the thrift npm package.

set -e

node_param=""
npm_param=""

if [ -n "$1" ]; then
    node_param="--host $1"
    npm_param="-- $node_param"
fi

until node util/check_thrift.js $node_param > /dev/null 2>&1; do
    >&2 echo "Backend is unavailable - sleeping"
    sleep 1
done

>&2 echo "Backend is up - executing command"

exec npm start $npm_param
