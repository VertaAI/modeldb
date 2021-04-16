# -*- coding: utf-8 -*-
# pylint: disable=undefined-variable

from datetime import datetime, timedelta, tzinfo
import time

ZERO = timedelta(0)

class UTC(tzinfo):
    """UTC"""

    def utcoffset(self, dt):
        return ZERO

    def tzname(self, dt):
        return "UTC"

    def dst(self, dt):
        return ZERO

utc = UTC()

UNIX_EPOCH = datetime(1970, 1, 1, tzinfo=utc)

def now():
    return datetime.now(utc)


def now_in_millis():
    return int(round(time.time() * 1000))


def epoch_millis(dt):
    if isinstance(dt, datetime):
        return int(round((dt - UNIX_EPOCH).total_seconds() * 1000))
    elif type(dt) == int:
        return dt
    else:
        raise ValueError("Cannot convert argument to epoch milliseconds")

def datetime_from_millis(millis):
    return UNIX_EPOCH + timedelta(milliseconds=millis)
