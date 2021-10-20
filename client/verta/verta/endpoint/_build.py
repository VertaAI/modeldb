# -*- coding: utf-8 -*-


class Build(object):
    EMPTY_MESSAGE = "no error message available"

    def __init__(self, id, status, message, _json):
        self._id = id
        self._status = status
        self._message = message
        self._json = _json

    def __repr__(self):
        return "Build({}, {})".format(self.id, repr(self.status))

    @classmethod
    def from_json(cls, response):
        return Build(
            response["id"], response["status"], response.get("message"), response
        )

    @property
    def id(self):
        return self._id

    @property
    def status(self):
        return self._status

    @property
    def message(self):
        return self._message or Build.EMPTY_MESSAGE

    @property
    def is_complete(self):
        return self.status in ("finished", "error")
