# First CLI arg is path to DB file and second is path to output dir.
`cp $1 modeldb.db`
`mkdir -p $2`
cp modeldb.db modeldb_temp.db
mvn compile
for size in 1 5 10 20 40 60 80 100 140 160 180 250 300 350 400
do
  filename="modeldb_duplicated_$size.db"
  if [ -e "$filename" ]
  then
    echo "[DUPLICATOR] $filename already exists"
    cp modeldb_duplicated_$size.db modeldb.db
  else
    echo "[DUPLICATOR] Creating $filename"
    cp modeldb_temp.db modeldb.db
    mvn exec:java -Dexec.mainClass="edu.mit.csail.db.ml.util.duplicator.DuplicatorMain" -Dexec.args="$size"
    cp modeldb.db modeldb_duplicated_$size.db
  fi
  mvn exec:java -Dexec.mainClass="edu.mit.csail.db.ml.evaluation.RunOperations" -Dexec.args="$2/output_$size.csv"
done
