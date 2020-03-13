#!/bin/sh

set -eo pipefail

docker build -t 493416687123.dkr.ecr.us-east-1.amazonaws.com/services/webapp:latest -f Dockerfile .
docker push 493416687123.dkr.ecr.us-east-1.amazonaws.com/services/webapp:latest
k delete pod $(k get pods | grep modeldb-webapp | cut -f1 -d' ')
