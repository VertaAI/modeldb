# TODO: move other argument-handling utils here
import functools

from . import _utils


def args_to_builtin(ignore_self):
    """
    Function decorator that applies ``_utils.to_builtin()`` to all arguments.

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
