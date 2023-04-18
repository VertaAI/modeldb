# -*- coding: utf-8 -*-

from verta._internal_utils import _utils


class Build:
    """
    An initiated docker build process of a deployable model.

    Represents an initiated docker build process. A build can be complete or in
    progress. Completed builds may be successful or failed. End-users of this
    library should not need to instantiate this class directly, but instead
    may obtain :class:`~verta.endpoint.Build` objects from methods such as
    :meth:`Endpoint.get_current_build<verta.endpoint.Endpoint.get_current_build>`.

    .. note::

        ``Build`` objects do not currently fetch live information from the
        backend; new objects must be obtained from public client methods to
        get up-to-date build statuses.

    Attributes
    ----------
    id : int
    status : str
    message : str
    is_complete : bool
    """

    _EMPTY_MESSAGE = "no error message available"

    def __init__(self, json):
        self._json = json

    def __repr__(self):
        return f'<Build (ID {self.id}, status "{self.status}")>'

    @classmethod
    def _get(cls, conn: _utils.Connection, workspace: str, id: int):
        url = "{}://{}/api/v1/deployment/workspace/{}/builds/{}".format(
            conn.scheme, conn.socket, workspace, id
        )
        response = _utils.make_request("GET", url, conn)
        _utils.raise_for_http_error(response)

        return cls(response.json())

    @property
    def id(self):
        """Get the ID of this build."""
        return self._json["id"]

    @property
    def status(self):
        """Get the status of this build."""
        return self._json["status"]

    @property
    def message(self):
        """Get an error message reported by this build if one exists."""
        return self._json.get("message", self._EMPTY_MESSAGE)

    @property
    def is_complete(self):
        """Return true if this build's status is finished or error."""
        return self.status in ("finished", "error")
