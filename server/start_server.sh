# generate thrift file
../scripts/gen_thrift_file.sh java '../thrift/ModelDB.thrift' './src/main/thrift/' 

# check if thrift version specified and pass that on
if [ -n "$1" ]; then
	THRIFT_VERSION="-Dthrift_version=$1"	
fi
# echo "mvn clean compile $THRIFT_VERSION && mvn exec:java -Dexec.mainClass=\"edu.mit.csail.db.ml.main.Main\" $THRIFT_VERSION"
mvn clean compile $THRIFT_VERSION && mvn exec:java -Dexec.mainClass="edu.mit.csail.db.ml.main.Main" $THRIFT_VERSION
