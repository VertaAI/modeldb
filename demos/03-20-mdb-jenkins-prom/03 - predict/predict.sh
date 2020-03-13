#!/bin/sh

set -e

curl -X post localhost:5000/predict -d '{"foo":1}' -H "Content-Type: application/json"