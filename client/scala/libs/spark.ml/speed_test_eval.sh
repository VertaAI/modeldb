# Read the arguments.
echo "Run this script as: ./speed_test_eval.sh <output_dir> <dataset_path> <imdb|housing|animal>" 
output_dir=$1
dataset_path=$2
dataset=$3

# Create the output directory if it doesn't exist.
mkdir -p $output_dir

# Build the project.
./build_client.sh

echo "[SPEED_EVAL] Killing server"
kill -9 $(lsof -t -i:6543)

echo "[SPEED_EVAL] Launching server"
cd ../../../../server
./reset.sh &
export server_pid=$!
cd ../client/scala/libs/spark.ml

echo "[SPEED_EVAL] Waiting for server to launch"
sleep 90

for iteration in {0..10}
do
  echo "[SPEED_EVAL] Running iteration $iteration"
  cmd="./evaluate.sh --path $dataset_path --dataset $dataset --workflow full --syncer true --min_num_rows 1 --outfile $output_dir/speed_eval_$iteration.csv"
  echo $cmd
  `$cmd`
done

echo "[SPEED_EVAL] Killing server"
kill -9 $(lsof -t -i:6543)
