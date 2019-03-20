#!/bin/bash
cd client
yarn install
yarn build
cd ..
yarn install
yarn start
