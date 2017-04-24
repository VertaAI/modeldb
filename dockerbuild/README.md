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

It is also possible to run ModelDB in Docker without Docker Compose.

1. **(Optional) Build images**

    You can build ModelDB's Docker images locally if you prefer, but prebuilt images are available on [MIT DBg's Docker Hub organization](https://hub.docker.com/r/mitdbg/) and will be downloaded automatically by Docker if you skip this step.

    ```bash
    docker build -t mitdbg/modeldb-backend -f dockerbuild/Dockerfile-backend .
    docker build -t mitdbg/modeldb-frontend -f dockerbuild/Dockerfile-frontend .
    ```

2. **Create a Docker network for ModelDB**

    Docker containers need a private network in order to find each other by name.

    ```bash
    docker network create modeldb
    ```

3. **Run MongoDB**

    ModelDB stores its data in MongoDB. If you have an existing MongoDB server you would like to use, you can skip this part and substitute the hostname or IP of your MongoDB server for 'mongo' in the next step.

    ```bash
        # Mongo server
        docker run -d \
            --name mongo \
            --net modeldb \
            -p 27017:27017 \
            mongo:3.4
    ```

4. **Run ModelDB**

    ```bash
    # ModelDB backend server ('mongo' tells it the hostname for mongo)
    docker run -d \
        --name backend \
        --net modeldb \
        -p 6543:6543 \
        mitdbg/modeldb-backend \
        mongo

    # ModelDB frontend server ('backend' tells it the hostname for backend)
    docker run -d \
        --name frontend \
        --net modeldb \
        -p 3000:3000 \
        mitdbg/modeldb-frontend \
        backend
    ```

    Shortly after running this command, ModelDB's frontend will be reachable at [http://localhost:3000](http://localhost:3000), and you will be able to log data into ModelDB (at port 6543) via ModelDB clients.
