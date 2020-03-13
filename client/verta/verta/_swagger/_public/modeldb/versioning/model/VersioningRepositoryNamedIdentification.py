# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningRepositoryNamedIdentification(BaseType):
  def __init__(self, name=None, workspace_name=None):
    required = {
      "name": False,
      "workspace_name": False,
    }
    self.name = name
    self.workspace_name = workspace_name

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('workspace_name', None)
    if tmp is not None:
      d['workspace_name'] = tmp

    return VersioningRepositoryNamedIdentification(**d)
