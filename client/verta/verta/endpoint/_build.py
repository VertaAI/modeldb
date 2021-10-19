import abc

from ..external import six


@six.add_metaclass(abc.ABCMeta)
class BuildSource(object):
    _FIELD = None

    def __init__(self, id):
        self._id = id

    @classmethod
    def from_http(cls, build_create):
        id = build_create[cls._field()]
        return cls(id)

    @classmethod
    def _field(cls):
        if cls._FIELD:
            return cls._FIELD
        else:
            raise NotImplementedError

    @property
    def id(self):
        return self._id


    def __repr__(self):
        return "{}({})".format(self.__class__.__name__, self.id)


class ExperimentRunSource(BuildSource):
    _FIELD = "run_id"


class ModelVersionSource(BuildSource):
    _FIELD = "model_version_id"


class ExternalLocationSource(BuildSource):
    _FIELD = "external_location"


class Build(object):
    def __init__(self, id, status, scan_status):
        pass

    @classmethod
    def from_http_response(cls, response):
        pass


# BuildStatus:
#   typ: string
#   enum:
#   - initializing
#   - building
#   - deleting
#   - error
#   - finished
#   - deleted
# BuildScanStatus:
#   typ: string
#   enum:
#     - unscanned
#     - scanning
#     - safe
#     - unsafe
#     - error
