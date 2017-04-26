../../scripts/gen_thrift_file.sh python '../../thrift/ModelDB.thrift' . 
thrift -r -out modeldb/thrift -gen py ModelDB.thrift 
