# -*- coding: utf-8 -*-

def extract_ids(objects_or_ids):
    try:
        ids = [obj.id for obj in objects_or_ids]
    except:
        ids = objects_or_ids
    return ids

def maybe(fn, val):
    if val:
        return fn(val)
    else:
        return None
