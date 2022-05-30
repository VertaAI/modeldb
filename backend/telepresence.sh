#!/bin/bash

set -eo pipefail

mkdir -p ~/telepresence_mounts

PORT_TO_INTERCEPT="${1:-8085}"

#TP2 only lets you do one port at a time; choose one. 8085 is GRPC and 8086 is HTTP
telepresence intercept modeldb--backend --env-json telepresence-env.json --port $PORT_TO_INTERCEPT:$PORT_TO_INTERCEPT --mount ~/telepresence_mounts/modeldb_backend --context engine-dev-admin
