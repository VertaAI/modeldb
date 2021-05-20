# -*- coding: utf-8 -*-

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
