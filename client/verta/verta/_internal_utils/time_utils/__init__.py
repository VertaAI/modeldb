# -*- coding: utf-8 -*-

from datetime import datetime, timedelta
import time

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


def datetime_from_millis(millis):
    return UNIX_EPOCH + timedelta(milliseconds=millis)
