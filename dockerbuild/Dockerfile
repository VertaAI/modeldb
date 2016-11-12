FROM ubuntu
RUN useradd -d /home/testuser -m testuser
RUN apt-get update
RUN apt-get install wget
RUN apt-get install unzip zip
RUN apt-get install git
RUN apt-get install software-properties-common
RUN add-apt-repository ppa:webupd8team/java
RUN apt-get update
RUN apt-get install openjdk-8-jdk
RUN apt-get install sqlite
RUN apt-get install maven
RUN apt-get install bzip2
RUN apt-get install automake bison flex g++ git libevent-dev libssl-dev libtool make pkg-config

RUN cd home
RUN cd testuser
RUN mkdir mdbDependencies
RUN cd mdbDependencies
RUN wget http://apache.mesi.com.ar/thrift/0.9.3/thrift-0.9.3.tar.gz
RUN wget https://repo.continuum.io/archive/Anaconda2-4.2.0-Linux-x86_64.sh
RUN wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.13/sbt-0.13.13.tgz
RUN wget http://d3kbcqa49mib13.cloudfront.net/spark-2.0.1-bin-hadoop2.7.tgz
RUN ./Anaconda2-4.2.0-Linux-x86_64.sh
RUN tar -xvzf sbt-0.13.13.tgz
RUN tar -xvzf spark-2.0.1-bin-hadoop2.7.tgz
RUN cd thrift-0.9.3
RUN ./configure
RUN cd ..


RUN PATH=/root/anaconda2/:$PATH
RUN PATH=/home/testuser/mdbDependencies/spark-2.0.1-bin-hadoop2.7/bin:$PATH
RUN PATH=/home/testuser/mdbDependencies/sbt-launcher-packaging-0.13.13/bin:$PATH
RUN PATH=/home/testuser/mdbDependencies/thrift-0.9.3:$PATH

RUN git clone https://github.com/mitdbg/modeldb.git
RUN cd modeldb
RUN cd server  
RUN cd codegen  
RUN ./gen_sqlite.sh  
RUN cd ..    
RUN ./start_server.sh &
RUN cd ..
RUN cd client/scala/libs/spark.ml 
RUN ./build_client.sh

CMD su testuser