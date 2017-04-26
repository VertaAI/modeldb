PATH=/root/anaconda2/:$PATH
PATH=/home/testuser/mdbDependencies/spark-2.0.1-bin-hadoop2.7/bin:$PATH
PATH=/home/testuser/mdbDependencies/sbt-launcher-packaging-0.13.13/bin:$PATH
PATH=/home/testuser/mdbDependencies/thrift-0.9.3:$PATH

cd home
cd testuser
cd mdbDependencies
./Anaconda2-4.2.0-Linux-x86_64.sh

cd ..
# This will be in the quiet section when there's no pass
git clone https://github.com/mitdbg/modeldb.git
cd modeldb
cd server  
cd codegen  
./gen_sqlite.sh  
cd ..    
./start_server.sh &
cd ..
cd client/scala/libs/spark.ml 
./build_client.sh
cd /home/testuser/modeldb
cd frontend
mkdir thrift
thrift -r -out thrift/ --gen js:node ../thrift/ModelDB.thrift
npm install
npm start &

cd /home/testuser/mdbDependencies
./zeppelin/bin/zeppelin-daemon.sh start



echo "Go to localhost:8082 to find ModelDB Zeppelin notebook (for Scala)."
echo "Go to locahost:8081 to find the front end"