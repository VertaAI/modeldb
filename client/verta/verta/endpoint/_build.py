# -*- coding: utf-8 -*-


class Build(object):
    """
    An initiated docker build process of a deployable model.

    Represents an initiated docker build process. A build can be complete or in
    progress. Completed builds may be successful or failed. End-users of this
    library should not need to instantiate this class directly, but instead
    may obtain :class:`~verta.endpoint.Build` objects from methods such as
    :meth:`Endpoint.get_current_build<verta.endpoint.Endpoint.get_current_build>`.

    Attributes
    ----------
    id : int
    status : str
    message : str, optional
    is_complete : bool
    """

    EMPTY_MESSAGE = "no error message available"

    def __init__(self, id, status, message, _json):
        self._id = id
        self._status = status
        self._message = message
        self._json = _json

    def __repr__(self):
        return "Build({}, {})".format(self.id, repr(self.status))

    @classmethod
    def _from_json(cls, response):
        return Build(
            response["id"], response["status"], response.get("message"), response
        )

    @property
    def id(self):
        """Get the ID of this build."""
        return self._id

    @property
    def status(self):
        """Get the status of this build."""
        return self._status

    @property
    def message(self):
        """Get an error message reported by this build if one exists."""
        return self._message or Build.EMPTY_MESSAGE

    @property
    def is_complete(self):
        """Return true if this build's status is finished or error."""
        return self.status in ("finished", "error")
