#!/bin/sh

base=$1
username=$2
if [ $# -ne 2 ]
then
  echo Usage: $0 basename username
  exit 1
fi

kubectl exec -it $base -- mkdir /tmp/backup
for table in artifact artifactstore attribute codeversion collaborator collaboratormappings comment dataset dataset_part_info datasetversion experiment experimentrun feature gitsnapshot gitsnapshotentity_filepaths job keyvalue observation path_dataset_version_info project query_dataset_version_info query_parameter raw_dataset_version_info tagmapping user_comment;
do kubectl exec -it $base -- pg_dump -t $table -U $username -h localhost --quote-all-identifiers -Fp --no-acl --no-owner  --data-only -f  /tmp/backup/$table.sql  postgres
done;
kubectl exec -it $base -- tar cvzf /tmp/backup.tgz /tmp/backup
kubectl cp $base:/tmp/backup.tgz $base.tgz
kubectl exec -it $base -- rm -rf /tmp/backup /tmp/backup.tgz
