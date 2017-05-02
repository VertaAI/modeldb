# Using Package documentation
Documentation for all the server code can be found in 

[`modeldb/docs/server/index.html`](server/index.html),

# Rebuilding Package documentation

If you would like to rebuild the documentation at any point,
there is a python utility that will generate the command and run it for you:

*(from the modeldb directory)*
```bash
cd docs/server
python build_docs.py
```

This will build the documentation to the *docs/server* directory.
