cp modeldb.db modeldb_temp.db
mvn compile
for size in 1 2 3
do
  cp modeldb_temp.db modeldb.db
  echo "Duplicating $size times"
  mvn exec:java -Dexec.mainClass="edu.mit.csail.db.ml.util.duplicator.DuplicatorMain" -Dexec.args="$size"
  cp modeldb.db modeldb_duplicated_$size.db
done
