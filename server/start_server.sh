../scripts/gen_thrift_file.py java '../thrift/ModelDB.thrift' './src/main/thrift/' 
mvn clean compile && mvn exec:java -Dexec.mainClass="edu.mit.csail.db.ml.main.Main"