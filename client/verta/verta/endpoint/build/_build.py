# -*- coding: utf-8 -*-

from datetime import datetime
from typing import List, Optional

from verta._internal_utils import _utils, time_utils
from . import _build_scan


class Build:
    """
    An initiated docker build process of a deployable model.

    .. versionchanged:: 0.23.0
        Moved from ``verta.endpoint.Build`` to ``verta.endpoint.build.Build``.
    .. versionadded:: 0.23.0
        The ``date_created`` property.

    Represents an initiated docker build process. A build can be complete or in
    progress. Completed builds may be successful or failed. End-users of this
    library should not need to instantiate this class directly, but instead
    may obtain :class:`Build` objects from methods such as
    :meth:`Endpoint.get_current_build() <verta.endpoint.Endpoint.get_current_build>`
    and :meth:`RegisteredModelVersion.list_builds() <verta.registry.entities.RegisteredModelVersion.list_builds>`.

    .. note::

        ``Build`` objects do not currently fetch live information from the
        backend; new objects must be obtained from public client methods to
        get up-to-date build statuses.

    Attributes
    ----------
    id : int
        Build ID.
    date_created : timezone-aware :class:`~datetime.datetime`
        The date and time when this build was created.
    status : str
        Status of the build (e.g. ``"building"``, ``"finished"``).
    message : str
        Message or logs associated with the build.
    is_complete : bool
        Whether the build is finished either successfully or with an error.

    """

    _EMPTY_MESSAGE = "no error message available"

    def __init__(self, conn, workspace, json):
        self._conn = conn
        self._workspace = workspace
        self._json = json

    def __repr__(self):
        return f'<Build (ID {self.id}, status "{self.status}")>'

    @classmethod
    def _get(cls, conn: _utils.Connection, workspace: str, id: int) -> "Build":
        url = f"{conn.scheme}://{conn.socket}/api/v1/deployment/workspace/{workspace}/builds/{id}"
        response = _utils.make_request("GET", url, conn)
        _utils.raise_for_http_error(response)

        return cls(conn, workspace, response.json())

    @classmethod
    def _list_model_version_builds(
        cls, conn: _utils.Connection, workspace: str, id: int
    ) -> List["Build"]:
        """Returns a model version's builds."""
        url = f"{conn.scheme}://{conn.socket}/api/v1/deployment/builds"
        data = {"model_version_id": id}
        response = _utils.make_request("GET", url, conn, params=data)
        _utils.raise_for_http_error(response)

        return [
            cls(conn, workspace, build_json)
            for build_json in response.json().get("builds", [])
        ]

    @property
    def id(self) -> int:
        return self._json["id"]

    @property
    def date_created(self) -> datetime:
        return time_utils.datetime_from_iso(self._json["date_created"])

    @property
    def status(self) -> str:
        return self._json["status"]

    @property
    def message(self) -> str:
        return self._json.get("message") or self._EMPTY_MESSAGE

    @property
    def is_complete(self) -> bool:
        return self.status in ("finished", "error")

    def get_scan(self) -> _build_scan.BuildScan:
        """Get this build's most recent scan.

        .. versionadded:: 0.23.0

        Returns
        -------
        :class:`~verta.endpoint.build.BuildScan`
            Build scan.

        """
        return _build_scan.BuildScan._get(self._conn, self.id)

    def start_scan(self, external: bool) -> _build_scan.BuildScan:
        """Start a new scan for this build. Internal scans are not yet supported.
        Use ``external=True`` parameter.

        This function only starts a scan; it does not wait for the finished scan
        to return results. Use
        :meth:`Build.get_scan() <verta.endpoint.build.Build.get_scan()>` to view
        progress and results of the scan.

        Parameters
        ----------
        external : bool
            True if using an external scan provider.

        Returns
        -------
        :class:`~verta.endpoint.build.BuildScan`
            Build scan.

        """
        if not external:
            raise NotImplementedError(
                "internal scans are not yet supported; please use `external=True` parameter"
            )
        return _build_scan.BuildScan._create(
            self._conn, self._workspace, self.id, external=external
        )
