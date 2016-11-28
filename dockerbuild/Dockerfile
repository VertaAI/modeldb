FROM ubuntu
RUN useradd -d /home/testuser -m testuser
RUN apt-get update
RUN echo Y | apt-get install wget unzip zip git software-properties-common
RUN add-apt-repository ppa:webupd8team/java
RUN apt-get update
RUN echo Y | apt-get install openjdk-8-jdk sqlite maven bzip2 automake bison flex g++ libevent-dev libssl-dev libtool make pkg-config

RUN ls && cd home/testuser && mkdir mdbDependencies && cd mdbDependencies && wget http://apache.mesi.com.ar/thrift/0.9.3/thrift-0.9.3.tar.gz && wget https://repo.continuum.io/archive/Anaconda2-4.2.0-Linux-x86_64.sh && wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.13/sbt-0.13.13.tgz && wget http://d3kbcqa49mib13.cloudfront.net/spark-2.0.1-bin-hadoop2.7.tgz && pwd && ls -l && chmod -R 777 ./Anaconda2-4.2.0-Linux-x86_64.sh && echo yes | ./Anaconda2-4.2.0-Linux-x86_64.sh && tar -xvzf sbt-0.13.13.tgz && tar -xvzf spark-2.0.1-bin-hadoop2.7.tgz && chmod -R 777 thrift-0.9.3 && cd thrift-0.9.3 && ./configure && cd ..

RUN PATH=/root/anaconda2/:$PATH && PATH=/home/testuser/mdbDependencies/spark-2.0.1-bin-hadoop2.7/bin:$PATH && PATH=/home/testuser/mdbDependencies/sbt-launcher-packaging-0.13.13/bin:$PATH && PATH=/home/testuser/mdbDependencies/thrift-0.9.3:$PATH && git clone https://github.com/mitdbg/modeldb.git && cd modeldb && cd server   && cd codegen   && ./gen_sqlite.sh   && cd ..     && ./start_server.sh & && cd .. && cd client/scala/libs/spark.ml  && ./build_client.sh

CMD su testuser