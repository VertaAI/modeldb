# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class StageResponse(BaseType):
  def __init__(self, components=None, creator_request=None, date_created=None, date_updated=None, id=None, status=None):
    required = {
      "components": False,
      "creator_request": False,
      "date_created": False,
      "date_updated": False,
      "id": False,
      "status": False,
    }
    self.components = components
    self.creator_request = creator_request
    self.date_created = date_created
    self.date_updated = date_updated
    self.id = id
    self.status = status

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .StageCreate import StageCreate

    
    
    
    from .StageStatusEnum import StageStatusEnum


    tmp = d.get('components', None)
    if tmp is not None:
      d['components'] = [ { "build_id": (lambda tmp: tmp)(tmp.get("build_id")), "ratio": (lambda tmp: tmp)(tmp.get("ratio")), "status": (lambda tmp: StageComponentStatusEnum.from_json(tmp))(tmp.get("status")), "message": (lambda tmp: tmp)(tmp.get("message")),  }  for tmp in tmp]
    tmp = d.get('creator_request', None)
    if tmp is not None:
      d['creator_request'] = StageCreate.from_json(tmp)
    tmp = d.get('date_created', None)
    if tmp is not None:
      d['date_created'] = tmp
    tmp = d.get('date_updated', None)
    if tmp is not None:
      d['date_updated'] = tmp
    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('status', None)
    if tmp is not None:
      d['status'] = StageStatusEnum.from_json(tmp)

    return StageResponse(**d)
