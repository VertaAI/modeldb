To run this project, first ensure that ModelDB Server is up and running. See instructions [here](server).

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

Note: unittests have been run with scikit-learn version 0.17.

Try running samples as in:
```
python samples/basic/BasicWorkflow.py
```
