# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbDeleteProjectResponse(BaseType):
  def __init__(self, status=None):
    required = {
      "status": False,
    }
    self.status = status

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('status', None)
    if tmp is not None:
      d['status'] = tmp

    return ModeldbDeleteProjectResponse(**d)
