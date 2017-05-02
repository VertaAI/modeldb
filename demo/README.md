# ModelDB Demo Site

These instructions are for creating the demo setup as on modeldb.csail.mit.edu, both the [frontend server](http://modeldb.csail.mit.edu:3000) and [client Jupyter notebooks](http://modeldb.csail.mit.edu:8000).

To set up a normal ModelDB installation, please follow the [Setup and Installation](../README.md#setup-and-installation) instructions in the main README.

## Overview

ModelDB is comprised of a server and client libraries to talk with it. Following the instructions below will provide a demo site allowing visitors to explore both.

## Requirements

The ModelDB demo site requires a Linux machine running Docker 1.13.0 or newer and Docker Compose 1.10.0 or newer.

It cannot run on Mac OS due to a limitation in Docker for Mac's networking. configurable-http-proxy and tmpnb use `--net host`, which does not work on Mac OS. The demo is currently untested on Windows.

## Architecture

The demo site is composed of:

- A single ModelDB server
    - MongoDB server
    - ModelDB Java backend
    - ModelDB Node.js frontend
- Multiple ephemeral Jupyter notebooks with ModelDB's Python client library
    - [configurable-http-proxy](https://github.com/jupyterhub/configurable-http-proxy)
    - [tmpnb](https://github.com/jupyter/tmpnb)

configurable-http-proxy handles incoming web requests and directs tmpnb to spawn new Jupyter notebooks for visitors. tmpnb is configured to spawn 10 concurrent notebooks and reclaim and restart notebooks which have been idle for more than 1 hour.

## Setup

The demo site uses Docker Compose to manage containers and their networks.

*These instructions create a blank demo server. If you want to preload data into the server, add your data as a ModelDB SQLite file named `modeldb.db` to the demo directory and add `-f docker-compose-preloaded.yml` to the below docker-compose calls, e.g. `docker-compose -f docker-compose-preloaded.yml build`.*

1. **Build images (optional)**

    The Docker images needed for the demo site are available from Docker Hub, but you can build the ModelDB-specific images from source yourself if you want the latest changes.

    ```bash
    cd [path_to_modeldb]/demo
    docker build -t mitdbg/modeldb-notebook -f Dockerfile-notebook ..
    docker-compose build
    ```

2. **Pull demo notebook image**

    If you didn't build mitdbg/modeldb-notebook yourself, you'll need to download it manually before running the demo site. Docker Compose unfortunately doesn't support build-only or download-only entries.

    ```bash
    docker pull mitdbg/modeldb-notebook
    ```

3. **Create secret token**

    configurable-http-proxy and tmpnb need a shared secret to prevent anything but tmpnb from adding routes to the proxy. Putting the token in a .env file will persist it across instances of the demo site. Docker Compose looks for .env and uses its contents for environment variable substitution when evaluating docker-compose.yml.

    ```bash
    cd [path_to_modeldb]/demo
    echo CONFIGPROXY_AUTH_TOKEN=$( head -c 30 /dev/urandom | xxd -p ) > .env
    ```

4. **Run the server**

    This will take a few minutes as it downloads any missing Docker images and spins up the various components of the demo site. When it is finished, ModelDB's frontend should be reachable at http://localhost:3000 and Jupyter notebooks at http://localhost:8000.

    Note: you will see many lines of `Socket error on boot: [Errno 111] Connection refused` from tmpnb_orchestrate while it waits for the first batch of Jupyter notebooks to spin up. Those errors should be ignored.

    ```bash
    cd [path_to_modeldb]/demo
    docker-compose up
    ```
