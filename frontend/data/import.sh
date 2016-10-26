#!/bin/bash

mongoimport --db modeldb-vis --collection models --drop --file ./models.json
mongoimport --db modeldb-vis --collection metrics --drop --file ./metrics.json
mongoimport --db modeldb-vis --collection configs --drop --file ./configs.json
mongoimport --db modeldb-vis --collection projects --drop --file ./projects.json