# -*- coding: utf-8 -*-

# TODO: move other argument-handling utils here
import functools
import numbers

from . import _utils


def args_to_builtin(ignore_self):
    """
    Returns a decorator that applies ``_utils.to_builtin()`` to all arguments.

    Parameters
    ----------
    ignore_self : bool
        Whether to ignore the first positional argument (i.e. ``self`` in
        methods).

    """
    def decorator(f):
        @functools.wraps(f)
        def wrapper(*args, **kwargs):
            if ignore_self:
                args = [args[0]] + list(map(_utils.to_builtin, args[1:]))
            else:
                args = list(map(_utils.to_builtin, args))

            kwargs = dict(zip(
                kwargs.keys(),
                map(_utils.to_builtin, kwargs.values()),
            ))

            return f(*args, **kwargs)
        return wrapper
    return decorator


def contains_only_numbers(l):
    """
    Returns whether `l` contains only numbers.

    Note that if `l` is an exhaustible iterator, it will be exhausted; Python
    doesn't seem to have a way to copy it while preserving the original
    referenced object.

    Uses ``numbers.Real``, which is a virtual superclass for ``int`` and
    ``float``.

    Parameters
    ----------
    l : 1D or 2D list of float
        List of numbers

    Returns
    -------
    bool

    """
    for row in l:
        try:
            items = iter(row)
        except TypeError:  # `row` is not iterable
            if not isinstance(row, numbers.Real):
                return False
        else:
            for item in items:
                if not isinstance(item, numbers.Real):
                    return False
    return True


def extract_id(object_or_id):
    """Return an id given either an object with an id or an id."""
    try:
        id = object_or_id.id
    except AttributeError:
        id = object_or_id
    return id


def extract_ids(objects_or_ids):
    """Return a list of ids given either objects with ids or a list of ids."""
    try:
        ids = [obj.id for obj in objects_or_ids]
    except:
        ids = objects_or_ids
    return ids


def maybe(fn, val):
    """Return fn(val) if val is not None, else None."""
    if val:
        return fn(val)
    else:
        return None
