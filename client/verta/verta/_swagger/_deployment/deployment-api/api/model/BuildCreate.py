# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class BuildCreate(BaseType):
  def __init__(self, run_id=None):
    required = {
      "run_id": True,
    }

    self.run_id = run_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('run_id', None)
    if tmp is not None:
      d['run_id'] = tmp

    return BuildCreate(**d)
