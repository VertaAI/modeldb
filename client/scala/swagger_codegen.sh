#!/bin/bash

set -e

BASE="../../protos/gen/swagger/protos"

rm -rf src/main/scala/ai/verta/swagger/_public

for f in $(find $BASE -type f | sort)
do
    echo "Processing $f"
    ../tools/swagger_codegen.py --input $f --output-dir src/main/scala/ai/verta/swagger --templates templates/swagger --file-suffix scala --case snake

    echo ""
done
