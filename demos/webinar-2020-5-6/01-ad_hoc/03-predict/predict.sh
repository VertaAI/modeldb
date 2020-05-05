#!/bin/sh

set -e

curl -X post localhost:5000/predict -d '["bedtime! why did u have to leave?"]' -H "Content-Type: application/json"