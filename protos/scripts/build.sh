#!/bin/bash
set -e

cd ..

GO_OUTPUT="gen/go/protos"
PYTHON_OUTPUT="gen/python/protos"
SWAGGER_OUTPUT="gen/swagger/protos"

for privacy in public
do
  cd protos/$privacy

  LOCAL_GO_OUTPUT="../../$GO_OUTPUT/$privacy"
  LOCAL_PYTHON_OUTPUT="../../$PYTHON_OUTPUT/$privacy"
  LOCAL_SWAGGER_OUTPUT="../../$SWAGGER_OUTPUT/$privacy"

  mkdir -p $LOCAL_GO_OUTPUT $LOCAL_PYTHON_OUTPUT $LOCAL_SWAGGER_OUTPUT

  GATEWAY_FLAGS="-I. -I/usr/local/include -I${GOPATH}/src/github.com/grpc-ecosystem/grpc-gateway/third_party/googleapis"
  PYTHON_GRPC_FLAGS="--grpc_python_out=$LOCAL_PYTHON_OUTPUT --python_out=$LOCAL_PYTHON_OUTPUT"

  for protodir in $(find . -name '*.proto' | xargs -n 1 dirname | sort | uniq)
  do
      echo $PWD
      echo "Building $protodir for python"
      python3 -m grpc_tools.protoc $PYTHON_GRPC_FLAGS $GATEWAY_FLAGS $protodir/*.proto

      echo "Building $protodir for golang"
      python3 -m grpc_tools.protoc $GATEWAY_FLAGS \
      --go_out=plugins=grpc,paths=source_relative:$LOCAL_GO_OUTPUT \
      --grpc-gateway_out=logtostderr=true,paths=source_relative,allow_delete_body=true:$LOCAL_GO_OUTPUT \
      --swagger_out=logtostderr=true,allow_delete_body=true:$LOCAL_SWAGGER_OUTPUT \
      $protodir/*.proto
      echo ''
  done

  echo "Fixing python folders"
  python3 ../../scripts/fix_imports.py --python-output-dir $LOCAL_PYTHON_OUTPUT

  echo "Fixing swagger definitions"
  for f in $(find $LOCAL_SWAGGER_OUTPUT -type f)
  do
      sed 's,"/v1/,"/,' $f > /tmp/foo.txt
      cat /tmp/foo.txt | jq '.basePath = "/v1"' > $f
  done

  cd ../..
done

cp -R $PYTHON_OUTPUT/* ../client/verta/verta/_protos

echo "Creating swagger definitions for scala"
pushd ../client/scala
./swagger_codegen.sh
popd
