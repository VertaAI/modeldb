#!/bin/bash

set -eo pipefail

mkdir -p ~/telepresence_mounts
telepresence --swap-deployment modeldb--backend --env-json telepresence-env.json --expose 8085 --expose 8086 --also-proxy 10.0.0.0/8 --mount ~/telepresence_mounts/modeldb_backend --context engine-dev-admin
