#!/bin/bash

set -exo pipefail

source ~/virtualenv/client/bin/activate

export VERTA_HOST="https://cm.dev.verta.ai"

# python data-versioning.py
python training.py
