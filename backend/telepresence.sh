#!/bin/bash

set -eo pipefail

mkdir -p ~/telepresence_mounts

# As of 5/20/2022 telepresence only supports a single port.  The last one listed is the one that is used.
# HTTP:
#telepresence intercept modeldb--backend --env-json telepresence-env.json --port 8086:8086 --mount ~/telepresence_mounts/modeldb_backend --context engine-dev-admin
# GRPC:
telepresence intercept modeldb--backend --env-json telepresence-env.json --port 8085:8085 --mount ~/telepresence_mounts/modeldb_backend --context engine-dev-admin
