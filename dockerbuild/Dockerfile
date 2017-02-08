FROM ubuntu
COPY installer.sh /
COPY quietInstaller.sh /
COPY interactiveInstaller.sh /
COPY addDependenciesToPath.sh /
COPY startModelDB.sh /
RUN /quietInstaller.sh
RUN echo "Please run /interactiveInstaller.sh"
CMD /bin/bash