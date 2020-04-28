#!/bin/bash

rm -f resolver.go
rm -f generated.go
cat definition/*.graphql > schema.graphql
go run github.com/99designs/gqlgen
