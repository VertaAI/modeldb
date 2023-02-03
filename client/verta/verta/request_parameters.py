# -*- coding: utf-8 -*-
"""Classes and functions related to request parameters used with the Verta Platform.

.. versionadded:: 0.20.0

"""

import abc

from verta.external import six

@six.add_metaclass(abc.ABCMeta)
class RequestParameters:
    ORG_ID_ENV = "ORG_ID_KEY"

    def __init__(self, org_id):
        self.org_id = org_id
