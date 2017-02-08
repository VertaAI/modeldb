PATH=/root/anaconda2/:$PATH
PATH=/home/testuser/mdbDependencies/spark-2.0.1-bin-hadoop2.7/bin:$PATH
PATH=/home/testuser/mdbDependencies/sbt-launcher-packaging-0.13.13/bin:$PATH
PATH=/home/testuser/mdbDependencies/thrift-0.9.3:$PATH
PATH=/home/testuser/mdbDependencies/thrift-0.9.3/compiler/cpp:$PATH

cd /home/testuser/modeldb/server
cd codegen
./gen_sqlite.sh
cd ..
./start_server.sh &
cd ../frontend
./start_frontend.sh &
cd /home/testuser/mdbDependencies
./zeppelin/bin/zeppelin-daemon.sh start
