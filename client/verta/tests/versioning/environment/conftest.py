# -*- coding: utf-8 -*-

import os
import tempfile

import pytest


@pytest.fixture
def requirements_file_without_versions(requirements_file):
    requirements = requirements_file.read().splitlines()
    stripped_requirements = [line.split("==")[0] for line in requirements]

    with tempfile.NamedTemporaryFile("w+") as tempf:
        tempf.write("\n".join(stripped_requirements))
        tempf.flush()  # flush object buffer
        os.fsync(tempf.fileno())  # flush OS buffer
        tempf.seek(0)

        yield tempf


@pytest.fixture
def requirements_file_with_unsupported_lines():
    with tempfile.NamedTemporaryFile("w+") as tempf:
        requirements = [
            "verta;python_version>=3.8",
            "--no-binary :all:",
            "--only-binary :none:",
            "--require-hashes",
            "--pre",
            "--trusted-host localhost:3000",
            "-c some_constraints.txt",
            "-f file://dummy",
            "-i https://pypi.org/simple",
            "-r more_requirements.txt",
            "en-core-web-sm==2.2.5",
        ]
        tempf.write("\n".join(requirements))
        tempf.flush()  # flush object buffer
        os.fsync(tempf.fileno())  # flush OS buffer
        tempf.seek(0)

        yield tempf
