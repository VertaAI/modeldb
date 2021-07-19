# -*- coding: utf-8 -*-

import logging
import os
import sys

import requests


logger = logging.getLogger(__name__)


URL_ENV_VAR = "VERTA_READTHEDOCS_WEBHOOK_URL"
TOKEN_ENV_VAR = "VERTA_READTHEDOCS_WEBHOOK_TOKEN"


def configure_logger():
    """Configure logger formatting and verbosity."""
    formatter = logging.Formatter("%(levelname)s - %(message)s")

    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(logging.DEBUG)
    handler.setFormatter(formatter)

    logger.setLevel(logging.DEBUG)
    logger.addHandler(handler)


def publish(branch="master"):
    url = os.environ[URL_ENV_VAR]
    token = os.environ[TOKEN_ENV_VAR]

    response = requests.post(url, json={"token": token, "branches": branch})
    response.raise_for_status()
    logger.info(response.json())


if __name__ == "__main__":
    configure_logger()
    publish()
