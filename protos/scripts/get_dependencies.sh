#!/bin/bash

set -e

go mod init foo

go get github.com/grpc-ecosystem/grpc-gateway@v1.14.6 github.com/golang/protobuf@v1.4.2

mkdir -p /root/go/src/github.com/golang/protobuf
cp -r /root/go/pkg/mod/github.com/golang/protobuf@v1.4.2/* /root/go/src/github.com/golang/protobuf

mkdir -p /root/go/src/github.com/grpc-ecosystem/grpc-gateway
cp -r /root/go/pkg/mod/github.com/grpc-ecosystem/grpc-gateway@v1.14.6/* /root/go/src/github.com/grpc-ecosystem/grpc-gateway

go get github.com/grpc-ecosystem/grpc-gateway/protoc-gen-grpc-gateway@v1.14.6
go get github.com/grpc-ecosystem/grpc-gateway/protoc-gen-swagger@v1.14.6
go get github.com/golang/protobuf/protoc-gen-go@v1.4.2
