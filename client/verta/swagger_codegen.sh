#!/bin/bash

set -e

BASE="../../protos/gen/swagger/protos"

rm -rf src/main/scala/ai/verta/swagger/_public

for f in $(find $BASE -type f | sort)
do
    echo "Processing $f"
    ../tools/swagger_codegen.py --input $f --output-dir verta/_swagger --templates templates/swagger --file-suffix py --case snake

    echo ""
done

for d in $(find verta/_swagger -type d)
do
    touch $d/__init__.py
done
