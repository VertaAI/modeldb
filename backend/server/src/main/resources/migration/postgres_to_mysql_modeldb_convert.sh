# !/bin/bash

set -e

if [ $# -ne 2 ]
then
	echo "Usage: $0 <path> <port>"
	exit 1
fi

header_size=23
footer_size=7
chunk_size=1000
mkdir -p result
tables=(artifact artifactstore attribute codeversion collaborator collaboratormappings comment dataset dataset_part_info datasetversion experiment experimentrun feature gitsnapshot gitsnapshotentity_filepaths job keyvalue observation path_dataset_version_info project query_dataset_version_info query_parameter raw_dataset_version_info tagmapping user_comment)

echo "Splitting tables"
for table in ${tables[@]}
do
	current_file_size=`< $1/$table.sql wc -l`
	good_size=`expr $current_file_size - $footer_size`
	echo table $table size `expr $good_size - $header_size + 1`
	cat $1/$table.sql | tail -n +$header_size | head -n -$footer_size | split -a 5 -l $chunk_size - result/$table.sql.
done

echo "Applying tables"
for table in ${tables[@]}
do
	echo table $table
	ls result/$table.sql.* | xargs -n 1 -P 50 -I LOCALFILE mysql --user=modeldb --port=$2 --local-infile --database=modeldb --protocol=TCP -e "LOAD DATA LOCAL INFILE 'LOCALFILE' INTO TABLE $table FIELDS TERMINATED BY '\t' ENCLOSED BY '' LINES TERMINATED BY '\n';"
done
