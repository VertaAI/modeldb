#!/bin/bash
declare -a dirnames=($1)

for dirname in "${dirnames[@]}"; do
    for filename in "${dirname}*_pb2*"; do
        sed -i '' 's|^from protos\.|from ...|g' $filename
    done
done