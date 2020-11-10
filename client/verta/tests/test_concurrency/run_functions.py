import random
import pytest
import os


def log_observation(client, i):
    observations = []
    run = client.set_experiment_run()

    for _ in range(5):
        observations.append(random.random())
        run.log_observation("obs", observations[-1])

    return {"run": run, "obs": observations}


def upload_artifact(client, i):
    run = client.set_experiment_run()

    PART_SIZE = int(5.4 * (10 ** 6))  # 5.4 MB; S3 parts must be > 5 MB
    os.environ['VERTA_ARTIFACT_PART_SIZE'] = str(PART_SIZE)

    filename = "file_{}.bin".format(i)
    FILE_CONTENTS = os.urandom(PART_SIZE * 2)
    with open(filename, 'wb') as f:
        f.write(FILE_CONTENTS)

    run.log_artifact("artifact", filename)

    return {"run": run, "artifact": FILE_CONTENTS}
