../../../../scripts/gen_thrift_file.py python '../../../../thrift/ModelDB.thrift' './thrift/' 
cd thrift/ 
thrift -r --gen py ModelDB.thrift
cd ..