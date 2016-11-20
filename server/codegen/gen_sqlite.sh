#!/usr/bin/env bash
mkdir -p jars
mkdir -p sqlite

dependencies=(jars/jooq-3.8.4.jar jars/jooq-meta-3.8.4.jar jars/jooq-codegen-3.8.4.jar sqlite/sqlite-jdbc-3.8.11.2.jar)
sources=(http://central.maven.org/maven2/org/jooq/jooq/3.8.4/jooq-3.8.4.jar http://central.maven.org/maven2/org/jooq/jooq-meta/3.8.4/jooq-meta-3.8.4.jar http://central.maven.org/maven2/org/jooq/jooq-codegen/3.8.4/jooq-codegen-3.8.4.jar http://central.maven.org/maven2/org/xerial/sqlite-jdbc/3.8.11.2/sqlite-jdbc-3.8.11.2.jar)

for ind in {0..3}
do
	if [ -f ${dependencies[$ind]} ];
	then
		 echo "File ${dependencies[$ind]} exists."
	else
		 echo "File ${dependencies[$ind]} does not exist. Fetching from Internet."
     wget ${sources[$ind]} -O ${dependencies[$ind]}
	fi
done

cat ./sqlite/createDb.sql | sqlite3 modeldb.db  &&
cat ./sqlite/createDb.sql | sqlite3 modeldb_test.db  &&
java -classpath jars/jooq-3.8.4.jar:jars/jooq-meta-3.8.4.jar:jars/jooq-codegen-3.8.4.jar:sqlite/sqlite-jdbc-3.8.11.2.jar:. org.jooq.util.GenerationTool sqlite/library.xml && 
mv modeldb.db ../
mv modeldb_test.db ../
chmod a+wrx ../modeldb.db
chmod a+wrx ../modeldb_test.db
