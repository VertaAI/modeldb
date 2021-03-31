import time


def now_in_millis():
    return int(round(time.time() * 1000))

def extract_ids(objects_or_ids):
    try:
        ids = [obj.id for obj in objects_or_ids]
    except:
        ids = objects_or_ids
    return ids
