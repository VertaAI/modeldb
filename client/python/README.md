# sklearn-modeldb-client

To run this project, 

First ensure ModelDB Server is up and running. See the ModelDB server's [repo](https://github.com/mitdbg/modeldb) for more information.

Then generate the gen-py folder by executing:

```
cd thrift/ 
thrift -r --gen py ModelDB.thrift 
```

To run the code, execute:

```
python -m client

``` 
