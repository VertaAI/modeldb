# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class StageTokenResponse(BaseType):
  def __init__(self, creator_request=None, id=None):
    required = {
      "creator_request": False,
      "id": False,
    }
    self.creator_request = creator_request
    self.id = id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .StageTokenCreate import StageTokenCreate

    

    tmp = d.get('creator_request', None)
    if tmp is not None:
      d['creator_request'] = StageTokenCreate.from_json(tmp)
    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp

    return StageTokenResponse(**d)
