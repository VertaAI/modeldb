# Running ModelDB with Docker

## Docker Compose

The easiest way to get a ModelDB server up and running is with Docker Compose.

1. **Install Docker Compose**

    [https://docs.docker.com/compose/install/](https://docs.docker.com/compose/install/)

2. **Run ModelDB**

    ```bash
    cd [path_to_modeldb]
    docker-compose up
    ```

`docker-compose up` will download prebuilt ModelDB images from Docker Hub and create and start containers. When it finishes, your ModelDB server should be reachable at [http://localhost/](http://localhost/).

## Manual Docker

It is also possiblet to run ModelDB in Docker without Docker Compose.

1. **(Optional) Build images**

    You can build ModelDB's Docker images locally if you prefer, but prebuilt images are available on [MIT DBg's Docker Hub organization](https://hub.docker.com/r/mitdbg/) and will be downloaded automatically by Docker if you skip this step.

    ```bash
    docker build -t mitdbg/modeldb-backend -f dockerbuild/Dockerfile-backend .
    docker build -t mitdbg/modeldb-frontend -f dockerbuild/Dockerfile-frontend .
    ```

2. **Set up ModelDB containers**

    ```bash
    # Create a private network for the services to see each other
    docker network create modeldb

    # Mongo server
    docker create \
        --name mongo \
        --net modeldb \
        -p 27017:27017 \
        mongo:3.4

    # ModelDB backend server ('mongo' tells it the hostname for mongo)
    docker create \
        --name backend \
        --net modeldb \
        -p 6543:6543 \
        mitdbg/modeldb-backend \
        mongo

    # ModelDB frontend server ('backend' tells it the hostname for backend)
    docker create \
        --name frontend \
        --net modeldb \
        -p 3000:3000 \
        mitdbg/modeldb-frontend \
        backend
    ```

3. **Run ModelDB**

    ```bash
    docker start mongo backend frontend
    ```

    Shortly after running this command, ModelDB frontend will be reachable at [http://localhost:3000](http://localhost:3000) and you will be able to log data into ModelDB (at port 6543) via ModelDB clients.
