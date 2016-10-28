# ../../../../scripts/gen_thrift_file.py python '../../../../thrift/ModelDB.thrift' './thrift/' 
#cd thrift/ 
thrift -r -out modeldb/thrift -gen py ModelDB.thrift 
#cd ..