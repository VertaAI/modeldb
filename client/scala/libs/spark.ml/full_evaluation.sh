# Read the arguments.
echo "Run this script as: ./full_evaluation.sh <output_dir> <imdb_path> <animal_path> <house_path>"
output_dir=$1
imdb_path=$2
animal_path=$3
house_path=$4
datasets=($imdb_path $animal_path $house_path)
datasetnames=("imdb" "animal" "housing")
workflows=("simple" "full" "exploratory")

# These are the tables in the database.
tables=(Annotation
MetricEvent
AnnotationFragment
ModelObjectiveHistory
CrossValidationEvent
PipelineStage
CrossValidationFold
Project
DataFrame
RandomSplitEvent
DataFrameColumn
TransformEvent
DataFrameSplit
Transformer
Event
TransformerSpec
Experiment
TreeLink
ExperimentRun
TreeModel
Feature
TreeModelComponent
FitEvent
TreeNode
GridCellCrossValidation
GridSearchCrossValidationEvent
HyperParameter
LinearModel
LinearModelTerm
)

# Create the output directory if it doesn't exist.
mkdir -p $output_dir

# Build the project.
./build_client.sh

# Evaluate many (workflow, dataset, dataset size) triples.
for dataset_size in 1 10000 20000 40000 70000 100000 200000 400000 700000 1000000
do
  for workflow in {0..2}
  do
    for dataset_index in {0..2}
    do
      echo "[FULL_EVAL] Evaluating (${workflows[$workflow]}, ${datasetnames[$dataset_index]}, $dataset_size)"

      echo "[FULL_EVAL] Killing server"
      kill -9 $(lsof -t -i:6543)

      echo "[FULL_EVAL] Launching server"
      cd ../../../../server
      ./reset.sh &
      export server_pid=$!
      cd ../client/scala/libs/spark.ml

      echo "[FULL_EVAL] Waiting for server to launch"
      sleep 90

      echo "[FULL_EVAL] Removing model files"
      rm -rf /tmp/animal*
      rm -rf /tmp/imdb*
      rm -rf /tmp/hous*

      echo "[FULL_EVAL] Growing dataset"
      python ../../../../scripts/evaluation/grow_dataset.py ${datasets[$dataset_index]} $dataset_size > dataset_tmp.csv

      echo "[FULL_EVAL] Launching test"
      cmd="./evaluate.sh --path dataset_tmp.csv --dataset ${datasetnames[$dataset_index]} --workflow ${workflows[$workflow]} --outfile $output_dir/${datasetnames[$dataset_index]}_${workflows[$workflow]}_$dataset_size.csv --syncer true  --min_num_rows 1"
      echo $cmd
      `$cmd`

      echo "[FULL_EVAL] Removing grown dataset"
      rm dataset_tmp.csv

      echo "[FULL_EVAL] Recording database size"
      du -h ../../../../server/modeldb.db > $output_dir/${datasetnames[$dataset_index]}_${workflows[$workflow]}_$dataset_size.dbsize

      echo "[FULL_EVAL] Recording table sizes"
      for i in {0..28}
      do
        printf "${tables[$i]}, " >> $output_dir/${datasetnames[$dataset_index]}_${workflows[$workflow]}_$dataset_size.tablesizes
        echo "SELECT COUNT(*) FROM ${tables[$i]};" | sqlite3 ../../../../server/modeldb.db >> $output_dir/${datasetnames[$dataset_index]}_${workflows[$workflow]}_$dataset_size.tablesizes
      done

      echo "[FULL_EVAL] Recording model files size"
      du -hcs /tmp/${datasetnames[$dataset_index]}* > $output_dir/${datasetnames[$dataset_index]}_${workflows[$workflow]}_$dataset_size.modelsize
    done
  done
done
