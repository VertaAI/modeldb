# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbDeleteExperimentRunTags(BaseType):
  def __init__(self, id=None, tags=None, delete_all=None):
    required = {
      "id": False,
      "tags": False,
      "delete_all": False,
    }
    self.id = id
    self.tags = tags
    self.delete_all = delete_all

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('tags', None)
    if tmp is not None:
      d['tags'] = [tmp for tmp in tmp]
    tmp = d.get('delete_all', None)
    if tmp is not None:
      d['delete_all'] = tmp

    return ModeldbDeleteExperimentRunTags(**d)
