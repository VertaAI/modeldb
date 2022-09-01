# -*- coding: utf-8 -*-

from datetime import timedelta, tzinfo  # pylint: disable=undefined-variable


class UTC(tzinfo):
    """UTC"""

    ZERO = timedelta(0)

    def utcoffset(self, dt):
        return self.ZERO

    def tzname(self, dt):
        return "UTC"

    def dst(self, dt):
        return self.ZERO


utc = UTC()
