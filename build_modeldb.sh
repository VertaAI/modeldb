#!/bin/bash
set -e

echo building modeldb images
./build_all_no_deploy_modeldb.sh

echo docker-compose -f docker-compose-all.yaml up
docker-compose -f docker-compose-all.yaml up
