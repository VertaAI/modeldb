#!/bin/sh

set -e

yarn run apollo codegen:generate   --endpoint=https://dev.verta.ai/api/v1/graphql/query --passthroughCustomScalars --customScalarsPrefix="GraphQL"  --target=typescript   --tagName=gql   --addTypename   --globalTypesFile=src/graphql-types/graphql-global-types.ts   graphql-types --header="Grpc-Metadata-developer_key: $VERTA_DEV_KEY" --header="Grpc-Metadata-email: $VERTA_EMAIL" --header='Grpc-Metadata-source: PythonClient'