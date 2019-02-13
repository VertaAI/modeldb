#!/bin/bash

set -e -o pipefail

# Set the following environment variables in the docker run
# AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_DEFAULT_REGION, AWS_DEFAULT_OUTPUT


# Inject AWS Secrets Manager Secrets
echo "Processing secrets [${SECRETS}]..."
vars=$(aws secretsmanager get-secret-value --secret-id ${SECRETS} --query SecretString --output text \
  | jq -r 'to_entries[] | "export \(.key)='\''\(.value)'\''"')

eval "$vars"
echo "$vars" > .env

set -xe
: "${REACT_APP_AUTH_DOMAIN?$REACT_APP_AUTH_DOMAIN}"
set -xe
: "${REACT_APP_AUTH_CLIENT_ID?$REACT_APP_AUTH_CLIENT_ID}"
set -xe
: "${REACT_APP_AUTH_CALLBACK_URL?$REACT_APP_AUTH_CALLBACK_URL}"

# REACT_APP_PROTOCOL='http'
# REACT_APP_DOMAIN='localhost'
# REACT_APP_PORT='8080'

# Run application
exec "$@"