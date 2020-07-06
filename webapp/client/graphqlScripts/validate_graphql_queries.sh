#!/bin/sh

set -e

cd .. && yarn graphql:download-schema && npx ts-graphql-plugin validate
