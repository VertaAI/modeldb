# -*- coding: utf-8 -*-

from datetime import datetime, timedelta
import time
import warnings

import pytimeparse

from ...external.six import PY2, PY3


if PY3:
    from ._utc_py3 import utc
if PY2:
    from ._utc_py2 import utc


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
    elif dt is None:
        return dt
    else:
        raise ValueError("Cannot convert argument to epoch milliseconds")


def _force_millisecond_resolution(delta):
    microseconds = delta.microseconds % 1000
    if microseconds > 0:
        warnings.warn(
            "Microsecond resolution unsupported, converting to millisecond resolution"
        )
        return delta - timedelta(microseconds=microseconds)
    else:
        return delta


def timedelta_millis(delta):
    if isinstance(delta, timedelta):
        delta = _force_millisecond_resolution(delta)
        return int(delta.total_seconds() * 1000)
    elif type(delta) is int and delta > 0:
        return delta
    raise ValueError("Cannot convert argument to duration milliseconds")


def datetime_from_millis(millis):
    return UNIX_EPOCH + timedelta(milliseconds=millis)


def parse_duration(value):
    duration = None
    if type(value) is str:
        try:
            dur_seconds = pytimeparse.parse(value)
            duration = timedelta(seconds=dur_seconds)
        except:
            raise ValueError("Cannot convert string argument to timedelta")
    if type(value) is int:
        if value < 0:
            raise ValueError("Cannot accept negative integer as a millisecond duration")
        duration = timedelta(milliseconds=value)
    if type(value) is timedelta:
        duration = value
    if duration:
        duration = _force_millisecond_resolution(duration)
        return duration
    raise ValueError("Cannot convert argument to a time duration")
