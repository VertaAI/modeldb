# BuildResponse:
#   type: object
#   properties:
#     id:
#       type: integer
#     status:
#       $ref: '#/definitions/BuildStatus'
#     scan_status:
#       $ref: '#/definitions/BuildScanStatus'
#     message:
#       type: string
#     date_created:
#       type: string
#       format: date-time
#     date_updated:
#       type: string
#       format: date-time
#     creator_request:
#       $ref: '#/definitions/BuildCreate'
#     location:
#       type: string
#
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

class Build(object):
    def __init__(self, id, status, _json):
        self._id = id
        self._status = status
        self._json = _json

    def __repr__(self):
        return "Build({}, {})".format(self.id, repr(self.status))

    @classmethod
    def from_json(cls, response):
        id = response["id"]
        status = response["status"]
        return Build(id, status, response)

    @property
    def id(self):
        return self._id

    @property
    def status(self):
        return self._status
