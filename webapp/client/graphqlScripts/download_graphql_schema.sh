#!/bin/sh

set -e

yarn run apollo client:download-schema  schema.graphql --endpoint=https://dev.verta.ai/api/v1/graphql/query    --header="Grpc-Metadata-developer_key: $VERTA_DEV_KEY" --header="Grpc-Metadata-email: $VERTA_EMAIL" --header='Grpc-Metadata-source: PythonClient'