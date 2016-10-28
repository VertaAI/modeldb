To run this project, 

First ensure ModelDB Server is up and running. See the ModelDB server's [repo](https://github.com/mitdbg/modeldb) for more information.

Run:
```
./build_client.sh
``` 
Put the python client on your PYTHONPATH
```
export PYTHONPATH=path_to_modedb_dir/client/python:$PYTHONPATH
```

Try running samples as in:
```
python samples/basic/BasicWorkflow.py
```
