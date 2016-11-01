To run this project, 

First ensure ModelDB Server is up and running. See the ModelDB server's [repo](https://github.com/mitdbg/modeldb/tree/master/server) for more information.

Run:
```
./build_client.sh
``` 
Put the python client on your PYTHONPATH
```
export PYTHONPATH=path_to_modedb_dir/client/python:$PYTHONPATH
```
Try running the unittests as:
```
python -m unittest discover modeldb/tests/sklearn/
```

Try running samples as in:
```
python samples/basic/BasicWorkflow.py
```
