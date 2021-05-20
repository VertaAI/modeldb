import os

import cloudpickle
from verta import Client

verta_host = os.environ["VERTA_HOST"]
run_id = os.environ["RUN_ID"]

client = Client(verta_host)
run = client.set_experiment_run(id=run_id)
commit, _ = run.get_commit()

# retrieve model
model = run.get_model()
with open("model.pkl", "wb") as f:
    cloudpickle.dump(model, f)

# retrieve Python version
env_ver = commit.get("env/python")
python_ver = ".".join(
    map(
        str,
        [
            env_ver._msg.python.version.major,
            env_ver._msg.python.version.minor,
        ],
    )
)
with open("Dockerfile", "r+") as f:
    contents = [line.strip() for line in f.readlines()]
    contents[0] == "FROM python:{}".format(python_ver)
    f.seek(0)
    f.write("\n".join(contents))

# retrieve Python package version pins
requirements = "\n".join(
    [
        "".join(
            [
                req.library,
                req.constraint,
                ".".join(
                    map(str, [req.version.major, req.version.minor, req.version.patch])
                )
                + req.version.suffix,
            ]
        )
        for req in env_ver._msg.python.requirements
    ]
)
with open("requirements.txt", "w") as f:
    f.write(requirements)
