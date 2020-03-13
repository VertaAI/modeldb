#!/bin/bash
set -e
ROOT_DIR=$PWD
BACKEND_DIR='backend'
BACKEND_PROXY_DIR='backend/proxy'
WEBAPP_DIR='webapp'
POSTGRES_DIR='data'

echo cd ${BACKEND_DIR}
cd ${BACKEND_DIR}
echo ./build.sh
./build.sh
echo cd ${ROOT_DIR}
cd ${ROOT_DIR}

echo cd ${BACKEND_PROXY_DIR}
cd ${BACKEND_PROXY_DIR}
echo make all
make all
echo cd ${ROOT_DIR}
cd ${ROOT_DIR}

echo cd ${WEBAPP_DIR}
cd ${WEBAPP_DIR}
echo ./build.sh
./build.sh
echo cd ${ROOT_DIR}
cd ${ROOT_DIR}

echo creating dir for database
mkdir -p ${POSTGRES_DIR}
