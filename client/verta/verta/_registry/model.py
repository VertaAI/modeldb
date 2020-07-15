# -*- coding: utf-8 -*-

from __future__ import print_function

from .._tracking.entity import _ModelDBEntity
from .._tracking.context import _Context
from .._internal_utils import _utils

from . import RegisteredModelVersion, RegisteredModelVersions


class RegisteredModel(_ModelDBEntity):
    def __init__(self, conn, conf, msg):
        raise NotImplementedError

    def __repr__(self):
        raise NotImplementedError

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name

    def get_or_create_version(self, name=None, desc=None, tags=None, attrs=None, id=None, time_created=None):
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        if id is not None:
            return RegisteredModelVersion._get_by_id(self._conn, self._conf, id)
        else:
            ctx = _Context(self._conn, self._conf)
            ctx.registered_model = self
            return RegisteredModelVersion._get_or_create_by_name(self._conn, name,
                                                       lambda name: RegisteredModelVersion._get_by_name(self._conn, self._conf, name, self.id),
                                                       lambda name: RegisteredModelVersion._create(self._conn, self._conf, ctx, name, desc=desc, tags=tags, attrs=attrs, date_created=time_created))

    def get_version(self, name=None, id=None):
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")
        if name is None and id is None:
            raise ValueError("must specify either `name` or `id`")

        if id is not None:
            return RegisteredModelVersion._get_by_id(self._conn, self._conf, id)
        else:

            return RegisteredModelVersion._get_by_name(self._conn, self._conf, name, self.id)

    @property
    def versions(self):
        return RegisteredModelVersions(self._conn, self._conf).with_model(self)

    @classmethod
    def _generate_default_name(cls):
        return "Model {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        raise NotImplementedError

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        raise NotImplementedError

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, desc=None, tags=None, attrs=None, date_created=None, public_within_org=None):
        raise NotImplementedError
