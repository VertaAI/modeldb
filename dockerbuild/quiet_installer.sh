useradd -d /home/testuser -m testuser
apt-get update
apt-get install -y wget
apt-get install -y unzip zip
apt-get install -y git
apt-get install -y software-properties-common
add-apt-repository ppa:webupd8team/java
apt-get update
apt-get install -y openjdk-8-jdk
apt-get install -y sqlite
apt-get install -y maven
apt-get install nodejs npm
apt-get install -y bzip2
apt-get install -y automake bison flex g++ git libevent-dev libssl-dev libtool make pkg-config

cd home
cd testuser
mkdir mdbDependencies
cd mdbDependencies
wget http://apache.mesi.com.ar/thrift/0.9.3/thrift-0.9.3.tar.gz
wget https://repo.continuum.io/archive/Anaconda2-4.2.0-Linux-x86_64.sh
wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.13/sbt-0.13.13.tgz
wget http://d3kbcqa49mib13.cloudfront.net/spark-2.0.1-bin-hadoop2.7.tgz
wget http://shinyfeather.com/zeppelin/zeppelin-0.6.2/zeppelin-0.6.2-bin-all.tgz
chmod 777 ./Anaconda2-4.2.0-Linux-x86_64.sh
tar -xvzf sbt-0.13.13.tgz
tar -xvzf spark-2.0.1-bin-hadoop2.7.tgz
tar -xvzf zeppelin-0.6.2-bin-all.tgz
tar -xvzf thrift-0.9.3.tar.gz
mv zeppelin-0.6.2-bin-all zeppelin
cp ./zeppelin/conf/zeppelin-site.xml.template ./zeppelin/conf/zeppelin-site.xml
sed -i -e 's/  <value>8080<\/value>/  <value>8082<\/value>/g' ./zeppelin/conf/zeppelin-site.xml
wget https://archive.ics.uci.edu/ml/machine-learning-databases/adult/adult.data
mkdir ./zeppelin/notebook/2C44QSZC4
wget https://raw.githubusercontent.com/mitdbg/modeldb-notebooks/master/scala/ModelDBSample.json -O ./zeppelin/notebook/2C44QSZC4/note.json
cd thrift-0.9.3
./configure
make
ln -n /home/testuser/mdbDependencies/thrift-0.9.3/compiler/cpp/thrift /usr/local/bin/thrift
cd /usr/bin
ln -s nodejs node
cd /


PATH=/root/anaconda2/:$PATH
PATH=/home/testuser/mdbDependencies/spark-2.0.1-bin-hadoop2.7/bin:$PATH
PATH=/home/testuser/mdbDependencies/sbt-launcher-packaging-0.13.13/bin:$PATH
PATH=/home/testuser/mdbDependencies/thrift-0.9.3:$PATH
PATH=/home/testuser/mdbDependencies/thrift-0.9.3/compiler/cpp:$PATH