# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class VersioningRepository(BaseType):
  def __init__(self, id=None, name=None, date_created=None, date_updated=None, workspace_id=None, workspace_type=None):
    required = {
      "id": False,
      "name": False,
      "date_created": False,
      "date_updated": False,
      "workspace_id": False,
      "workspace_type": False,
    }
    self.id = id
    self.name = name
    self.date_created = date_created
    self.date_updated = date_updated
    self.workspace_id = workspace_id
    self.workspace_type = workspace_type

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    
    from .WorkspaceTypeEnumWorkspaceType import WorkspaceTypeEnumWorkspaceType


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('date_created', None)
    if tmp is not None:
      d['date_created'] = tmp
    tmp = d.get('date_updated', None)
    if tmp is not None:
      d['date_updated'] = tmp
    tmp = d.get('workspace_id', None)
    if tmp is not None:
      d['workspace_id'] = tmp
    tmp = d.get('workspace_type', None)
    if tmp is not None:
      d['workspace_type'] = WorkspaceTypeEnumWorkspaceType.from_json(tmp)

    return VersioningRepository(**d)
