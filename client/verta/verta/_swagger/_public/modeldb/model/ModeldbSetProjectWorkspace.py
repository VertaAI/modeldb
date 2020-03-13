# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbSetProjectWorkspace(BaseType):
  def __init__(self, id=None, workspace_name=None):
    required = {
      "id": False,
      "workspace_name": False,
    }
    self.id = id
    self.workspace_name = workspace_name

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('workspace_name', None)
    if tmp is not None:
      d['workspace_name'] = tmp

    return ModeldbSetProjectWorkspace(**d)
