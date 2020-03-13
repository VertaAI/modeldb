#!/bin/sh

set -e

docker build . -t modeldb-protos
docker run --rm -v $(dirname $PWD | xargs dirname):/mount modeldb-protos /bin/bash -c 'cd /mount/protos/scripts && ./build.sh'