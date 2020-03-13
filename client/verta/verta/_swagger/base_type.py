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

    def to_json(self):
        d = dict(self)
        for k, v in d.items():
            if isinstance(v, BaseType):
                d[k] = v.to_json()

        d = {k: v for k, v in d.items() if v is not None}

        return d
