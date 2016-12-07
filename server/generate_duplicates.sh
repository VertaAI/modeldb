cp modeldb.db modeldb_temp.db
mvn compile
for size in 1 5 10 20 40 60 80 100
do
  filename="modeldb_duplicated_$size.db"
  if [ -e "$filename" ]
  then
    echo "[DUPLICATOR] $filename already exists"
  else
    echo "[DUPLICATOR] Creating $filename"
    cp modeldb_temp.db modeldb.db
    mvn exec:java -Dexec.mainClass="edu.mit.csail.db.ml.util.duplicator.DuplicatorMain" -Dexec.args="$size"
    cp modeldb.db modeldb_duplicated_$size.db
  fi
done
