## Setup

Follow setup instructions in ../server directory

Start server in one terminal

    $ cd modeldb/server/
    $ ./start_server.sh

Generate thrift files

    $ cd modeldb/frontend/
    $ mkdir thrift
    $ thrift -r -out thrift/ --gen js:node ../thrift/ModelDB.thrift 
        
Start up node server

    $ npm install
    $ npm start

Visit application at localhost:3000
