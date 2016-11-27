## Setup

Make sure the ModelDB server is running, following the instructions in the server/ directory. 

Generate thrift files

    $ cd modeldb/frontend/
    $ mkdir thrift
    $ thrift -r -out thrift/ --gen js:node ../thrift/ModelDB.thrift 
        
Start up node server

    $ npm install
    $ npm start

Visit application at localhost:3000
