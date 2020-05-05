# Prerequisites

- Python requirements (`python -m pip install -r requirements.txt`)
- ModelDB (`docker-compose -f docker-compose-all.yaml pull` at this repository's root)
- [Docker](https://docs.docker.com/get-docker/)
- [Jenkins](https://www.jenkins.io/download/)

1. [Run ModelDB](https://github.com/VertaAI/modeldb#up-and-running-in-5-minutes).
1. Run Jenkins (`jenkins-lts --httpPort=7070`, since ModelDB occupies port `8080`).
1. Run Jupyter (`jupyter notebook`).

# Ad Hoc
The ad hoc workflow logs model ingredients to S3 and fetches them, one by one, to deploy them. This can lead to mismatched artifacts if S3 buckets are not carefully managed, and it can be difficult to track what results are associated with what model.

1. Run [the notebook](https://github.com/VertaAI/modeldb/blob/master/demos/webinar-2020-5-6/01-ad_hoc/01-train/NLP%20training.ipynb), logging model ingredients to S3.
1. Run [the Jenkins pipeline](https://github.com/VertaAI/modeldb/blob/master/demos/webinar-2020-5-6/01-ad_hoc/02-package/s3-build.Jenkinsfile), building a Docker image to serve the model.
    1. Or run [`02-package/run.sh`](https://github.com/VertaAI/modeldb/blob/master/demos/webinar-2020-5-6/01-ad_hoc/02-package/run.sh) directly, setting the environment variables `BUCKET`, `MODEL_PATH`, and `METADATA_PATH` to point at the model ingredients on S3.
1. Run [`03-predict/run.sh`](https://github.com/VertaAI/modeldb/blob/master/demos/webinar-2020-5-6/01-ad_hoc/03-predict/run.sh) to serve the model.
    1. Run [`03-predict/predict.sh`](https://github.com/VertaAI/modeldb/blob/master/demos/webinar-2020-5-6/01-ad_hoc/03-predict/predict.sh) to make predictions against the model at `localhost:5000`.
    1. View live metrics from the model in your web browser at `http://localhost:9090/`.

# ModelDB Versioning
The ModelDB Versioning workflow leverages our versioning system to snapshot model ingredients together, linking them to experimental results and enabling reproducibility, reverts, and merges of promising ingredients.

1. Run [the notebooks](https://github.com/VertaAI/modeldb/tree/master/demos/webinar-2020-5-6/02-mdb_versioned/01-train), logging model ingredients to S3.
1. Run [the Jenkins pipeline](https://github.com/VertaAI/modeldb/blob/master/demos/webinar-2020-5-6/02-mdb_versioned/02-package/mdb-build.Jenkinsfile), building a Docker image to serve the model.
    1. Or run [`02-package/run.sh`](https://github.com/VertaAI/modeldb/blob/master/demos/webinar-2020-5-6/02-mdb_versioned/02-package/run.sh) directly, setting the environment variables `VERTA_HOST` and `RUN_ID` to fetch the associated model ingredients from ModelDB.
1. Run [`03-predict/run.sh`](https://github.com/VertaAI/modeldb/blob/master/demos/webinar-2020-5-6/02-mdb_versioned/03-predict/run.sh) to serve the model.
    1. Run [`03-predict/predict.sh`](https://github.com/VertaAI/modeldb/blob/master/demos/webinar-2020-5-6/02-mdb_versioned/03-predict/predict.sh) to make predictions against the model at `localhost:5000`.
        - Try a few German phrases, as well!
          ```python
          [
              "Guten Morgen.",               # Good morning.
              "Gute Nacht.",                 # Good night!
              "Sie sind sehr freundlich.",   # You're very kind!

              "Da muss ich widersprechen.",  # I disagree.
              "Es ist ein Notfall!",         # It's an emergency!
              "Ich verstehe nicht.",         # I don't understand.
              "Ich bin sauer.",              # I'm angry.
          ]
          ```
    1. View live metrics from the model in your web browser at http://localhost:9090/.
