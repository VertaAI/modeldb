#!/bin/bash

set -e

commands=()
commands+=('verta --help')
commands+=('verta registry --help')
commands+=('verta registry list --help')
commands+=('verta registry list model --help')
commands+=('verta registry list model --filter tag=foo')
commands+=('verta registry list model BERT --filter tag=foo')
commands+=('verta registry get --help')
commands+=('verta registry get model --help')
commands+=('verta registry get model BERT')
commands+=('verta registry get model BERT latest')
commands+=('verta registry create --help')
commands+=('verta registry create model --help')
commands+=('verta registry add --help')
commands+=('verta registry add model --help')

for ((i = 0; i < ${#commands[@]}; i++))
do
    cmd="${commands[$i]}"
    echo $cmd
    eval $cmd
    echo "================="
done
