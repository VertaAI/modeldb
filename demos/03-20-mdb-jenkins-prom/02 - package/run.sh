#!/bin/sh

set -e

python fetch_model.py

docker build -t model .