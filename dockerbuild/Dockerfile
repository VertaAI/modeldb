FROM ubuntu
COPY installer.sh /
COPY quietInstaller.sh /
COPY interactiveInstaller.sh /
RUN /quietInstaller.sh
RUN echo "Please run /interactiveInstaller.sh"
CMD /bin/bash