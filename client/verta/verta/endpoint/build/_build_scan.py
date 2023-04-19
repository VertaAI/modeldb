# -*- coding: utf-8 -*-

from datetime import datetime
from enum import Enum

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
    date_updated : :class:`~datetime.datetime`
        The date and time when this scan was performed/updated.
    progress : :class:`ScanProgressEnum`
        The current progress of this scan.
    status : :class:`ScanStatusEnum`
        The result of this scan.
    passed : bool
        Whether this scan passed. This property is for convenience, equivalent to

        .. code-block:: python

            self.status == "safe"
    failed : bool
        Whether this scan failed. This property is for convenience, equivalent to

        .. code-block:: python

            self.status in {"unknown", "unsafe"}

    """

    def __init__(self, json):
        self._json = json

    @classmethod
    def _get(cls, conn: _utils.Connection, build_id: int):
        url = "{}://{}/api/v1/deployment/builds/{}/scan".format(
            conn.scheme, conn.socket, build_id
        )
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
    def status(self) -> ScanStatusEnum:
        return ScanStatusEnum(self._json["safety_status"])

    @property
    def passed(self) -> bool:
        return self.status == ScanStatusEnum.SAFE

    @property
    def failed(self) -> bool:
        # based on the web app's notion of failure
        # https://github.com/VertaAI/VertaWebApp/blob/1e66c73/webapp/client/src/features/catalog/registeredModelVersion/release/view/VulnerabilityScanCard/VulnerabilityScanInfo.tsx#L22-L45
        return self.status in {ScanStatusEnum.UNKNOWN, ScanStatusEnum.UNSAFE}
