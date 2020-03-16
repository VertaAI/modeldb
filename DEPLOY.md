# Setup and Installation

There are multiple way to bring up ModelDB.

## Docker Setup

### Deploy pre published images

If you have [Docker Compose](https://docs.docker.com/compose/install/) installed, you can bring up a ModelDB server with just a single command.

```bash
docker-compose -f docker-compose-all.yaml up
```

This command will fetch the published images from Docker hub and setup the multi container environment. The webapp can be accessed at **<http://localhost:3000>**.

Logs will have an entry similar to `Backend server started listening on 8085` to indicate backend is up. During the first run backend will have to run the liquibase scripts so it will take a few extra minutes to come up. The progress can be monitored in the logs.

*Once the command finishes it might take a couple of minutes for the proxy, backend and frontend to establish connection. During this time any access through frontend or client may result in 502.*

### Build images from source and deploy

To build the images you need Docker and jdk(1.8) installed. Each of the modules has a script to build its Docker image. This flow can be triggered by running from the root of the repository

```bash
./build_all_no_deploy_modeldb.sh
```

This will build the Docker images locally.

To use these images run steps in [Deploy pre published images](#deploy-pre-published-images), but this time since there will be locally built images , those will be used instead of pulling the images from remote repository.

A utility script to combine the two steps is available and can be run as

```bash
./build_modeldb.sh
```

## Kubernetes Setup

Helm chart is available at `chart/modeldb`. ModelDB can be brought up on a Kubernetes cluster by running:

```bash
cd chart/modeldb
helm install . --name <release-name> --namespace <k8s namespace>
```

By default, the `default` namespace on your Kubernetes cluster is used. `release-name` is a arbitrary identifier user picks to perform future helm operations on the cluster.

To bring a cluster down, run:

```bash
helm del --purge <release-name-used-install-cmd>
```

## AWS ami

An pre-baked ami `packer-ubuntu-18.04-amd64-minikube-1.14.9-oss-1583957384` with modeldb running on minikube is made available on AWS. You can launch a EC2 machine with the image to spin up a deployment of ModelDB, accessible at http://<public hostname or ip>: 30080.
