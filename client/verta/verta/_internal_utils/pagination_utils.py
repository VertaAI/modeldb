# -*- coding: utf-8 -*-


def page_limit_from_proto(page_limit):
    """
    Translates the proto value for `page_limit` to the client equivalent.

    For user-friendliness, the client stores ``None`` to mean "no page limit:
    return everything" whereas the our backend API uses ``-1``.

    Parameters
    ----------
    page_limit : int
        Page limit from a protobuf message.

    Returns
    -------
    page_limit : int or None

    """
    if page_limit == -1:
        return None
    else:
        return page_limit


def page_limit_to_proto(page_limit):
    """
    Translates the client value for `page_limit` to the proto equivalent.

    For user-friendliness, the client stores ``None`` to mean "no page limit:
    return everything" whereas the our backend API uses ``-1``.

    Parameters
    ----------
    page_limit : int or None
        Page limit from a client object.

    Returns
    -------
    page_limit : int

    """
    if page_limit is None:
        return -1
    else:
        return page_limit
