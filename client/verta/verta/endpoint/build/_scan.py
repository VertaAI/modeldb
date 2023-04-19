# -*- coding: utf-8 -*-

from dataclasses import dataclass
from datetime import datetime
from enum import Enum

from verta._internal_utils import _utils, time_utils


class ScanProgress(str, Enum):
    UNSCANNED = "unscanned"
    SCANNING = "scanning"
    SCANNED = "scanned"
    ERROR = "error"


class ScanStatus(str, Enum):
    UNKNOWN = "unknown"
    SAFE = "safe"
    UNSAFE = "unsafe"


class BuildScan:
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
    def progress(self) -> ScanProgress:
        return ScanProgress(self._json["scan_status"])

    @property
    def status(self) -> ScanStatus:
        return ScanStatus(self._json["safety_status"])

    @property
    def passed(self) -> bool:
        return self.status == ScanStatus.SAFE

    @property
    def failed(self) -> bool:
        # based on the web app's notion of failure
        # https://github.com/VertaAI/VertaWebApp/blob/1e66c73/webapp/client/src/features/catalog/registeredModelVersion/release/view/VulnerabilityScanCard/VulnerabilityScanInfo.tsx#L22-L45
        return self.status in {ScanStatus.UNKNOWN, ScanStatus.UNSAFE}
