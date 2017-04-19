../scripts/gen_thrift_file.py java '../thrift/ModelDB.thrift' './src/main/thrift/' 

# check if thrift version specified and pass that on
if [ -n "$1" ]; then
    THRIFT_VERSION="-Dthrift_version=$1"    
fi
mvn clean compile $THRIFT_VERSION && mvn exec:java -Dexec.mainClass="edu.mit.csail.db.ml.main.Main" -Dexec.args="-conf ./src/main/resources/reference-test.conf" $THRIFT_VERSION