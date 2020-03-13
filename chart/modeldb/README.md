# ModelDB: A system to manage ML models

ModelDB is an end-to-end system for managing machine learning models. It ingests models and associated metadata as models are being trained, stores model data in a structured format, and surfaces it through a web-frontend for rich querying.

### Prerequisites

- [Kubernetes](https://kubernetes.io/docs/home/) version 1.8+
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
- [Helm](https://helm.sh/)

### Deploying ModelDB on Kubernetes

In this directory run:

```
helm install . --name <release-name> --namespace <k8s namespace>
```
By default, the "default" namespace on your Kubernetes cluster is used.

### What next?

Now that you have modelDB up and running on your K8s cluster, please visit
[our user guide and documentation](https://verta.readthedocs.io/en/docs/index.html) to get started.

### Using Custom Images

To build and deploy each of the services running as a part of modelDB, please follow the instructions in the
corresponding service's repository to build the docker image for that service.
Once the image is pushed to a container registry, update the corresponding property to point to the newly
developed image in the [values.yaml](https://github.com/VertaAI/modeldb/chart/modeldb/values.yaml) file.

### Contributing

To contribute to our project, look at the contributing section for each of the components -
* [modeldb-client](https://github.com/VertaAI/modeldb/tree/master/client/README.md)
* [modeldb-backend](https://github.com/VertaAI/modeldb/tree/master/backend/README.md)
* [modeldb-frontend](https://github.com/VertaAI/modeldb/tree/master/webapp/README.md)
