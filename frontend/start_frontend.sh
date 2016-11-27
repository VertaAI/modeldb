mkdir -p './thrift'
thrift -r -out './thrift' -gen js:node '../thrift/ModelDB.thrift'

npm install
npm start