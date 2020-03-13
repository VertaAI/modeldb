# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbAddExperimentTag(BaseType):
  def __init__(self, id=None, tag=None):
    required = {
      "id": False,
      "tag": False,
    }
    self.id = id
    self.tag = tag

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('tag', None)
    if tmp is not None:
      d['tag'] = tmp

    return ModeldbAddExperimentTag(**d)
