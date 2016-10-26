## Setup

Start mongodb in a separate terminal window

    $ sudo mongod
    
Import dummy data

    $ cd frontend/data
    $ chmod u+x import.sh
    $ ./import.sh
    
Start up node server

    $ cd ..
    $ npm install
    $ npm start
