# Usage

To execute one of the scripts here, do the following:

Make sure your ModelDB server is running.

Then, start the spark shell.

```
spark-shell -i <script_name> --jars <path/to/sparkclientjar.jar>
```

Then, run the script.

```
Main.run("path/to/data")
```
