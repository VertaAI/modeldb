# ModelDB: A system to track, version and audit Machine Learning models

----

ModelDB is an end-to-end system for tracking, versioning and auditing  machine learning models. It ingests models and associated metadata as models are being trained, stores model data in a structured format, and surfaces it through a web-frontend for rich querying and the python client.

This version of ModelDB is built upon its [predecessor](https://mitdbg.github.io/modeldb/) from [CSAIL, MIT](https://www.csail.mit.edu/). The previous version can be found on Github [here](https://github.com/mitdbg/modeldb).

----
<p align="center">
  <a href="https://hub.docker.com/u/vertaaiofficial">
    <img src="https://img.shields.io/docker/v/vertaaiofficial/modeldb-backend?color=534eb5&label=Docker%20image%20version&style=plastic" alt="docker hub" />
  </a>
  <a href="https://pypi.org/project/verta/">
    <img src="https://img.shields.io/pypi/v/verta?color=534eb5&style=plastic" alt="PyPI" />
  </a>
  <a href="https://anaconda.org/conda-forge/verta">
    <img src="https://img.shields.io/conda/v/conda-forge/verta?color=534eb5&style=plastic" alt="Conda" />
  </a>
  <a href="https://github.com/VertaAI/modeldb/blob/master/LICENSE">
    <img src="https://img.shields.io/pypi/l/verta?color=534eb5&style=plastic" alt="License" />
  </a>
  <br>
  <a href="https://hub.docker.com/u/vertaaiofficial">
    <img src="https://img.shields.io/docker/pulls/vertaaiofficial/modeldb-backend?color=534eb5&style=plastic" alt="docker hub" />
  </a>
  <a href="https://pypi.org/project/verta/">
    <img src="https://img.shields.io/pypi/dm/verta?color=534eb5&label=PyPI%20Downloads&style=plastic" alt="PyPI" />
  </a>
  <a href="https://github.com/VertaAI/modeldb/graphs/commit-activity">
    <img src="https://img.shields.io/github/commit-activity/w/vertaai/modeldb?color=534eb5&style=plastic" alt="Commits" />
  </a>
  <a href="https://github.com/VertaAI/modeldb/graphs/commit-activity">
    <img src="https://img.shields.io/github/last-commit/vertaai/modeldb?color=534eb5&style=plastic" alt="Last Commit" />
  </a>
  <br>
  <a href="https://github.com/VertaAI/modeldb/graphs/commit-activity">
    <img src="https://img.shields.io/github/stars/vertaai/modeldb?style=social" alt="Forks" />
  </a>
  <a href="https://twitter.com/intent/follow?screen_name=VertaAI">
    <img src="https://img.shields.io/twitter/follow/VertaAI?label=VertaAI&style=social" alt="Twitter" />
  </a>
  <a href="http://bit.ly/modeldb-mlops">
    <img src="https://cdn.brandfolder.io/5H442O3W/as/pl546j-7le8zk-5guop3/Slack_RGB.png" alt="Slack" height =30px/>
  </a>
</p>

----

<h3 align="center">
  <a href="#quick-start">Quick-start</a>
  <span> · </span>
  <a href="https://docs.verta.ai/en/master/guides/workflow.html">Workflow</a>
  <span> · </span>
  <a href="https://docs.verta.ai/en/master/guides/examples.html">Examples</a>
  <span> · </span>
  <a href="https://github.com/VertaAI/modeldb/blob/master/client/CONTRIBUTING.md">Contribute</a>
  <span> · </span>
  <a href="http://bit.ly/modeldb-mlops">Support</a>
</h3>

----

## What’s In This Document

- [Quick-start](#-quick-start)
- [Community](#-community)
- [Architecture](#-architecture)
- [How to Contribute](#-how-to-contribute)
- [License](#-license)
- [Thanks to Our Contributors](#-thanks)

----

## Quick-start

If you have [Docker Compose](https://docs.docker.com/compose/install/) installed, you can bring up a ModelDB server with just a single command.

```bash
docker-compose -f docker-compose-all.yaml up
```

This command will fetch the published images from Docker hub and setup the multi container environment. The webapp can be accessed at **<http://localhost:3000>**.

Logs will have an entry similar to `Backend server started listening on 8085` to indicate backend is up. During the first run backend will have to run the liquibase scripts so it will take a few extra minutes to come up. The progress can be monitored in the logs.

*Once the command finishes it might take a couple of minutes for the proxy, backend and frontend to establish connection. During this time any access through frontend or client may result in 502.*

**Other ways to deploy ModelDB are:**

1. [Building the source code and deploying](DEPLOY.md#build-images-from-source-and-deploy)
1. [Deploy on kubernetes using help](DEPLOY.md#kubernetes-setUp)
1. [Spin up a AWS EC2 machine using a modeldb ami](DEPLOY.md#AWS)

----

## Community

For Getting Started guides, Tutorials, and API reference [docs](https://docs.verta.ai/en/master/).

To report a bug, file a documentation issue, or submit a feature request, please open a GitHub issue.

For help, questions, contribution discussions and release announcements, please join us in [Slack](http://bit.ly/modeldb-mlops).

----

## Architecture

At a high level the architecture of ModelDB in a Kubernetes cluster or a Docker application looks as below:

![image](doc-resources/images/modeldb-architecture.png)

- **ModelDB Client** developed in Python which can instantiated in the user's model building code and exposes functions to log related information to ModelDB.
- **ModelDB Frontend**  developed in JavaScript and typescript is the visual reporting module of ModelDB. It also acts as an entry point for the ModelDB cluster.
  - It receives the request from client (1) and the browser and route them to the appropriate container.
  - The gRPC calls (2) for creating, reading,updating or deleting Projects, Experiments, ExperimentRuns, Dataset, DatasetVersions or their metadata are routed to ModelDB Proxy.
  - The HTTP calls (3) for storing and retrieving binary artifacts are forwarded directly to backend.
- **ModelDB Backend Proxy** developed in golang is a light weight gRPC to Http convertor.
  - It receives the gRPC request from the front end (2) and sends them to backend (4). In the other direction it converts the response from backend and sends it to the frontend.
- **ModelDB Backend** developed in java is module which stores, retrieves or deletes information as triggered by user via the client or the front end.
  - It exposes gRPC endpoints (4) for most of the operations which is used by the proxy.
  - It has http endpoints (3) for storing, retrieving and deleting artifacts used directly by the frontend.
- **Database** ModelDB Backend stores (5) the information from the requests it receive into a Relational database.
  - Out of the box ModelDB is configured and verified to work against PostgreSQL, but since it uses Hibernate as a ORM and liquibase for change management, it should be easy to configure ModelDB to run on another SQL Database supported by the the tools.

*Volumes : The relational database and the artifact store in backend need volumes attached to enable persistent storage.*

### Repo Structure

Each module in the architecture diagram has a designated folder in this repository, and has their own README covering in depth documentation and contribution guidelines.

1. **protos** has the protobuf definitions of the objects and endpoint used across ModelDB. More details [here](protos/README.md).
1. **backend** has the source code and tests for ModelDB Backend. It also holds the proxy at **backend/proxy**. More details [here](backend/README.md).
1. **client** has the source code and tests for ModelDB client. More details [here](client/README.md).
1. **webapp** has the source and tests for ModelDB frontend. More details [here](webapp/README.md).

Other supporting material for deployment and documentation is at:

1. **chart** has the helm chart to deploy ModelDB onto your Kubernetes cluster. More details [here](chart/modeldb/README.md).
1. **doc-resources** has images for documentation.

----

## Contributions

As seen from the [Architecture](#architecture) ModelDB provides a full stack solution to tracking, versioning and auditing  machine learning models.
We are open to contributions to any of the modules in form of Pull Requests. 

The main skill sets for each module are as below:

1. backend : If you are interested in `Java` development or are interested in database design using technologies like `Hibernate` and `Liquibase` please take a look at [backed README](backend/README.md) for setup and development instructions.
1. client : If you are interested in `Python` or `Scala` development or are interested in building examples notebooks on various ML frameworks logging data to Modeldb please take a look at [client CONTRIBUTING](client/CONTRIBUTING.md) for contribution instructions.
1. protos : If you are interested  in `Node`,`React` or `Redux`based development please take a look at [webapp README](webapp/README.md)

----

## License

ModelDB is licensed under Apache 2.0.

----

## Thanks

Thanks to our many [contributors](CONTRIBUTORS.md) and users.
