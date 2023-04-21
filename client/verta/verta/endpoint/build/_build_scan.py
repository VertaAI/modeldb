# -*- coding: utf-8 -*-

from datetime import datetime
from enum import Enum
from typing import Optional

from verta._internal_utils import _utils, time_utils


class ScanProgressEnum(str, Enum):
    """The current progress of a build scan.

    For all intents and purposes, this can be treated as a :class:`str`.

    Examples
    --------
    .. code-block:: python

        assert build.get_scan().progress == "scanned"

    """

    UNSCANNED = "unscanned"
    SCANNING = "scanning"
    SCANNED = "scanned"
    ERROR = "error"


class ScanStatusEnum(str, Enum):
    """The result of a build scan.

    For all intents and purposes, this can be treated as a :class:`str`.

    Examples
    --------
    .. code-block:: python

        assert build.get_scan().status == "safe"

    """

    UNKNOWN = "unknown"
    SAFE = "safe"
    UNSAFE = "unsafe"


class BuildScan:
    """A scan of a Verta model build.

    There should not be a need to instantiate this class directly; please use
    :meth:`Build.get_scan() <verta.endpoint.build.Build.get_scan>` instead.

    Attributes
    ----------
    date_updated : timezone-aware :class:`~datetime.datetime`
        The date and time when this scan was performed/updated.
    progress : :class:`ScanProgressEnum`
        The current progress of this scan.
    status : :class:`ScanStatusEnum` or None
        The result of this scan. ``None`` is returned if this scan is not yet
        finished, and therefore has no status.
    passed : bool
        Whether this scan finished and passed. This property is for
        convenience, equivalent to

        .. code-block:: python

            build_scan.status == "safe"

    """

    def __init__(self, json):
        self._json = json

    def __repr__(self):
        detail_str = f'progress "{self.progress.value}"'
        if self.status is not None:
            detail_str += f', status "{self.status.value}"'

        return f"<BuildScan ({detail_str})>"

    @classmethod
    def _get(cls, conn: _utils.Connection, build_id: int):
        url = f"{conn.scheme}://{conn.socket}/api/v1/deployment/builds/{build_id}/scan"
        response = _utils.make_request("GET", url, conn)
        _utils.raise_for_http_error(response)

        return cls(response.json())

    @property
    def date_updated(self) -> datetime:
        return time_utils.datetime_from_iso(self._json["date_updated"])

    @property
    def progress(self) -> ScanProgressEnum:
        return ScanProgressEnum(self._json["scan_status"])

    @property
    def status(self) -> Optional[ScanStatusEnum]:
        if self.progress != ScanProgressEnum.SCANNED:
            return None
        return ScanStatusEnum(self._json["safety_status"])

    @property
    def passed(self) -> bool:
        return self.status == ScanStatusEnum.SAFE
