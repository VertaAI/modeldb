# -*- coding: utf-8 -*-

from datetime import datetime, timedelta, timezone

UNIX_EPOCH = datetime(1970, 1, 1, tzinfo=timezone.utc)

def now():
    return datetime.now(timezone.utc)

def epoch_millis(dt):
    if isinstance(dt, datetime):
        return round((dt - UNIX_EPOCH).total_seconds() * 1000)
    elif type(dt) == int:
        return dt
    else:
        raise ValueError("Cannot convert argument to epoch milliseconds")

def datetime_from_millis(millis):
    return UNIX_EPOCH + timedelta(milliseconds=millis)
