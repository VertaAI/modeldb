#!/bin/bash
if [ "$EUID" -ne 0 ] 
then
  printf "Install must be run as root: sudo su -\n"
  exit
fi

if ! yum -q repolist | grep -qw bintray;
then
  printf "sbt-bintray repository is missing - required for sbt install\n"
fi

if ! yum -q repolist | grep -qw HDP; 
then
  printf "Hadoop repository is missing - required for spark install\n"
fi

if ! yum -q repolist | grep -qw base; 
then
  printf "RedHat/CentOS base repository is missing\n"
fi

if ! yum -q repolist | grep -qw epel; 
then
  printf "EPEL repository is missing - see https://fedoraproject.org/wiki/EPEL\n"
fi

firewall-cmd --zone=public --add-port=6543/tcp --permanent
firewall-cmd --zone=public --add-port=3000/tcp --permanent

yum update -y
yum install -y wget
yum install -y unzip
yum install -y zip
yum install -y git
yum install -y java-1.8.0-openjdk
yum install -y sqlite
yum install -y maven
yum install -y thrift
yum install -y sbt
yum install -y bzip2
yum install -y automake bison flex gcc-c++ libevent-devel libtool make openssl-devel pkgconfig
yum install -y nodejs npm
yum install -y spark

cd /root
wget https://repo.continuum.io/archive/Anaconda2-4.2.0-Linux-x86_64.sh
./Anaconda2-4.2.0-Linux-x86_64.sh -b
export PATH=/root/anaconda2/:$PATH

git clone https://github.com/mitdbg/modeldb.git
cd modeldb/server/codegen
./gen_sqlite.sh  
cd ..    
./start_server.sh &
cd ../client/scala/libs/spark.ml 
./build_client.sh
cd ../../../python
./build_client.sh
cd ../../frontend
./start_frontend.sh &

printf "Ensure proxies, if required, are set for NPM and Maven! See 'npm config set [proxy|https-proxy]' and the file ~/.m2/settings.xml\n"
