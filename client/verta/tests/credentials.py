# -*- coding: utf-8 -*-

import os

from . import constants

HOST = os.environ.get(constants.HOST_ENV_VAR, constants.DEFAULT_HOST)
PORT = os.environ.get(constants.PORT_ENV_VAR, constants.DEFAULT_PORT)
EMAIL = os.environ.get(constants.EMAIL_ENV_VAR, constants.DEFAULT_EMAIL)
DEV_KEY = os.environ.get(constants.DEV_KEY_ENV_VAR, constants.DEFAULT_DEV_KEY)
EMAIL_2 = os.environ.get(constants.EMAIL_2_ENV_VAR)
DEV_KEY_2 = os.environ.get(constants.DEV_KEY_2_ENV_VAR)
EMAIL_3 = os.environ.get(constants.EMAIL_3_ENV_VAR)
DEV_KEY_3 = os.environ.get(constants.DEV_KEY_3_ENV_VAR)
