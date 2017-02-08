#Running the docker file
Use the following command to download the image from dockerhub:
```bash
docker run -it -p 8082:8082 -p 8081:3000 sanjayganeshan/mdgmodeldb:latest
```

OR build it from the dockerfile. Note that is is **HIGHLY** recommended to use the version from dockerhub.

*from the dockerbuild directory*

```bash
docker build .
docker run -it -p 8082:8082 -p 8081:3000 <ID>
```

You should replace *ID* with the ID that is printed after building.

Once you have started the built container, please run `interactiveInstaller.sh` to install the dependencies that require
accepting license agreements. It will then also build ModelDB. You should commit this as a new tag so that you do not need to wait for
it to install again. Future runs can then `startModelDB.sh` to build modelDB (You can also build modelDB manually, just use `addDependenciesToPath.sh`
to add the dependencies to your PATH first).

##Summary of Files

`installer.sh`: A full-fledged installer that installs ModelDB. You shouldn't have to run this.
`interactiveInstaller.sh`: An installer that installs the components of ModelDB that require the user to accept an agreement,
then starts ModelDB.
`quietInstaller.sh`: An installer that installs all it can without user input
`startModelDB.sh`: A script that adds the dependencies to the PATH, then starts ModelDB.
`addDependenciesToPath.sh`: A script that adds the dependencies to the PATH.
`Dockerfile`: A script that tells Docker how to build the image.
