HOST="https://socure.app.verta.ai"

# Create dockerfile for project
from verta import Client
client = Client(HOST)

repo = client.get_or_create_repository("My repo")
master = repo.get_commit()

env = master.get("project1/environment")
version = "{}.{}".format(env._msg.python.version.major, env._msg.python.version.minor)

requirements = ""
if env._msg.python.requirements:
    requirements = "\n".join([
                env._req_spec_msg_to_str(req_spec_msg)
                for req_spec_msg
                in sorted(
                    env._msg.python.requirements,
                    key=lambda req_spec_msg: req_spec_msg.library,
                )
    ])

constraints = ""
if env._msg.python.constraints:
    constraints = "\n".join([
                env._req_spec_msg_to_str(req_spec_msg)
                for req_spec_msg
                in sorted(
                    env._msg.python.constraints,
                    key=lambda req_spec_msg: req_spec_msg.library,
                )
    ])

code = master.get("project1/code")
code_repo = code._msg.git.repo
code_ref = code._msg.git.hash
# Uncomment if you want to use a branch instead of a locked commit
# code_ref = code._msg.git.branch

dockerfile = """
FROM python:{version}
COPY requirements.txt requirements.txt
COPY constraints.txt constraints.txt
RUN pip install -r requirements.txt -c constraints.txt
COPY code_repo /code_repo
CMD ["python", "/code_repo/project_dockerfile/train.py"]
""".format(version=version)

build_script = """#!/bin/bash

set -xe

git clone {code_repo} code_repo || true
cd code_repo
git fetch
git checkout {code_ref}
cd ..
docker build -t example-image .
""".format(code_repo=code_repo, code_ref=code_ref)

with open("requirements.txt", "w") as f:
    f.write(requirements)
with open("constraints.txt", "w") as f:
    f.write(constraints)
with open("Dockerfile", "w") as f:
    f.write(dockerfile)
with open("build.sh", "w") as f:
    f.write(build_script)
