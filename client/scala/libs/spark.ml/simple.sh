#!/usr/bin/env bash
AWS_PROFILE=default $SPARK_HOME/bin/spark-submit --master local[*] --class "edu.mit.csail.db.ml.modeldb.sample.SimpleSample" target/scala-2.11/modeldb-scala-client.jar $@
