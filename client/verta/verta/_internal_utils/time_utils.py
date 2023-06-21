# -*- coding: utf-8 -*-

from datetime import datetime, timedelta, timezone
import time
import warnings

import pytimeparse
from verta._vendored import six


UNIX_EPOCH = datetime(1970, 1, 1, tzinfo=timezone.utc)


def now():
    return datetime.now(timezone.utc)


def now_in_millis():
    return int(round(time.time() * 1000))


def epoch_millis(dt):
    if isinstance(dt, datetime):
        dt = _promote_naive_to_utc(dt)
        return int(round((dt - UNIX_EPOCH).total_seconds() * 1000))
    elif isinstance(dt, int) and dt >= 0:
        return dt
    elif dt is None:
        return dt
    else:
        raise ValueError("Cannot convert argument to epoch milliseconds")


def _promote_naive_to_utc(dt):
    if dt.tzinfo is None:
        warnings.warn("Time zone naive datetime found, assuming UTC time zone")
        return dt.replace(tzinfo=timezone.utc)
    else:
        return dt


def _force_millisecond_resolution(delta):
    microseconds = delta.microseconds % 1000
    if microseconds > 0:
        warnings.warn(
            "Microsecond resolution unsupported, converting to millisecond resolution"
        )
        return delta - timedelta(microseconds=microseconds)
    else:
        return delta


def duration_millis(delta):
    if isinstance(delta, timedelta):
        # NB: The following explicitly avoids floating point errors
        # which occur when computing millis via delta.total_seconds() * 1000
        delta = _force_millisecond_resolution(delta)
        millis_from_seconds = int(delta.total_seconds()) * 1000
        millis_from_micro = int(delta.microseconds / 1000)
        return millis_from_seconds + millis_from_micro
    elif isinstance(delta, int) and delta >= 0:
        return delta
    raise ValueError("cannot convert argument to duration milliseconds")


def datetime_from_millis(millis):
    return UNIX_EPOCH + timedelta(milliseconds=millis)


def datetime_from_iso(date_string) -> datetime:
    """Basically the std lib's :func:`datetime.datetime.fromisoformat`.

    Additionally has a handler for ISO 8601's Z suffix as returned by some of
    our backends, which isn't supported until Python 3.11 [#]_.

    Examples
    --------
    .. code-block:: python

        dt = datetime_from_iso("2011-11-04T00:05:23Z")
        expected_dt = datetime(2011, 11, 4, 0, 5, 23, tzinfo=timezone.utc)
        assert dt == expected_dt

    References
    ----------
    .. [#] https://docs.python.org/3.11/library/datetime.html#datetime.datetime.fromisoformat

    """
    if date_string[-1] == "Z":
        date_string = date_string[:-1] + "+00:00"
    return datetime.fromisoformat(date_string)


def parse_duration(value):
    duration = None
    if isinstance(value, six.string_types):
        try:
            dur_seconds = pytimeparse.parse(six.ensure_str(value))
            duration = timedelta(seconds=dur_seconds)
        except:
            raise ValueError("cannot convert string argument to timedelta")
    if isinstance(value, six.integer_types):
        if value < 0:
            raise ValueError("cannot accept negative integer as a millisecond duration")
        duration = timedelta(milliseconds=value)
    if isinstance(value, timedelta):
        duration = value
    if duration is not None:
        duration = _force_millisecond_resolution(duration)
        return duration
    raise ValueError("cannot convert argument to a time duration")
