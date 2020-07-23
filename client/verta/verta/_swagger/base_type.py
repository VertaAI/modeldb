import json

class BaseType(dict):
    def __init__(self):
        pass

    def __setattr__(self, name, value):
        self[name] = value

    def __delattr__(self, name):
        del self[name]

    def __getattr__(self, name):
        if name in self:
            return self[name]
        else:
            raise AttributeError

    @staticmethod
    def _clean_json(obj):
        if isinstance(obj, dict):
            return {k: v for k, v in obj.items() if v is not None}
        return obj

    def _to_json_walk(self, obj, keep_fields):
        if isinstance(obj, BaseType) and self != obj:
            obj = obj.to_json()
        elif isinstance(obj, dict):
            obj = {k: self._to_json_walk(v) for k,v in obj.items()}
        elif isinstance(obj, list):
            obj = [self._to_json_walk(v) for v in obj]

        if not keep_fields:
            obj = BaseType._clean_json(obj)

        return obj

    def to_json(self, keep_fields=False):
        return self._to_json_walk(self, keep_fields)
