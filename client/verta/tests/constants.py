# -*- coding: utf-8 -*-

import os
import sys


HOST_ENV_VAR = "VERTA_HOST"
PORT_ENV_VAR = "VERTA_PORT"
EMAIL_ENV_VAR = "VERTA_EMAIL"
DEV_KEY_ENV_VAR = "VERTA_DEV_KEY"
EMAIL_2_ENV_VAR = "VERTA_EMAIL_2"
DEV_KEY_2_ENV_VAR = "VERTA_DEV_KEY_2"
EMAIL_3_ENV_VAR = "VERTA_EMAIL_3"
DEV_KEY_3_ENV_VAR = "VERTA_DEV_KEY_3"
HTTPS_VERTA_URL_ENV_VAR = "VERTA_HTTPS_URL"

DEFAULT_HOST = None
DEFAULT_PORT = None
DEFAULT_EMAIL = None
DEFAULT_DEV_KEY = None

HOST = os.environ.get(HOST_ENV_VAR, DEFAULT_HOST)
PORT = os.environ.get(PORT_ENV_VAR, DEFAULT_PORT)
EMAIL = os.environ.get(EMAIL_ENV_VAR, DEFAULT_EMAIL)
DEV_KEY = os.environ.get(DEV_KEY_ENV_VAR, DEFAULT_DEV_KEY)
EMAIL_2 = os.environ.get(EMAIL_2_ENV_VAR)
DEV_KEY_2 = os.environ.get(DEV_KEY_2_ENV_VAR)
EMAIL_3 = os.environ.get(EMAIL_3_ENV_VAR)
DEV_KEY_3 = os.environ.get(DEV_KEY_3_ENV_VAR)

# for virtualenv tests
PYTHON_VERSION_SEGMENT = "python{}.{}".format(
    sys.version_info.major,
    sys.version_info.minor,
)
LIB_SITE_PACKAGES = os.path.join("lib", PYTHON_VERSION_SEGMENT, "site-packages")
LIB32_SITE_PACKAGES = os.path.join("lib32", PYTHON_VERSION_SEGMENT, "site-packages")
LIB64_SITE_PACKAGES = os.path.join("lib64", PYTHON_VERSION_SEGMENT, "site-packages")
BIN_PYCACHE = os.path.join("bin", "__pycache__")
