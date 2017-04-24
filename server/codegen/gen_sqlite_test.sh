#!/usr/bin/env bash
rm -rf jars/
rm sqlite/*jar
mkdir -p jars
wget http://central.maven.org/maven2/org/jooq/jooq/3.8.4/jooq-3.8.4.jar -O jars/jooq-3.8.4.jar
wget http://central.maven.org/maven2/org/jooq/jooq-meta/3.8.4/jooq-meta-3.8.4.jar -O jars/jooq-meta-3.8.4.jar
wget http://central.maven.org/maven2/org/jooq/jooq-codegen/3.8.4/jooq-codegen-3.8.4.jar -O jars/jooq-codegen-3.8.4.jar
wget http://central.maven.org/maven2/org/xerial/sqlite-jdbc/3.15.1/sqlite-jdbc-3.15.1.jar -O sqlite/sqlite-jdbc-3.15.1.jar
cat ./sqlite/createDb.sql | sqlite3 modeldb.db  &&
cat ./sqlite/createDb.sql | sqlite3 modeldb_test.db  &&
java -classpath jars/jooq-3.8.4.jar:jars/jooq-meta-3.8.4.jar:jars/jooq-codegen-3.8.4.jar:sqlite/sqlite-jdbc-3.15.1.jar:. org.jooq.util.GenerationTool sqlite/library.xml && 
mv modeldb.db ../
mv modeldb_test.db ../
chmod a+wrx ../modeldb.db
chmod a+wrx ../modeldb_test.db

# clear old modeldb mongodb data and start mongodb server
mongo modeldb_metadata_test --eval "db.dropDatabase()"
mkdir -p mongodb
mongod --dbpath mongodb &
