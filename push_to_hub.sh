#!/bin/bash

if [ "$#" -ne 1 ]; then
read -p "No tag provided. pushing images as :latest. Are you sure? " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
    tag="latest"
fi
else
tag=$1
fi

echo "updating images with tag $tag"

docker tag vertaaiofficial/modeldb-backend:latest vertaaiofficial/modeldb-backend:$tag
docker tag vertaaiofficial/modeldb-proxy:latest vertaaiofficial/modeldb-proxy:$tag
docker tag vertaaiofficial/modeldb-frontend:latest vertaaiofficial/modeldb-frontend:$tag
docker tag vertaaiofficial/modeldb-graphql:latest vertaaiofficial/modeldb-graphql:$tag

docker push vertaaiofficial/modeldb-backend:$tag
docker push vertaaiofficial/modeldb-proxy:$tag
docker push vertaaiofficial/modeldb-frontend:$tag
docker push vertaaiofficial/modeldb-graphql:$tag
