# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbProject(BaseType):
  def __init__(self, id=None, name=None, description=None, date_created=None, date_updated=None, short_name=None, readme_text=None, project_visibility=None, workspace_id=None, workspace_type=None, attributes=None, tags=None, owner=None, code_version_snapshot=None, artifacts=None):
    required = {
      "id": False,
      "name": False,
      "description": False,
      "date_created": False,
      "date_updated": False,
      "short_name": False,
      "readme_text": False,
      "project_visibility": False,
      "workspace_id": False,
      "workspace_type": False,
      "attributes": False,
      "tags": False,
      "owner": False,
      "code_version_snapshot": False,
      "artifacts": False,
    }
    self.id = id
    self.name = name
    self.description = description
    self.date_created = date_created
    self.date_updated = date_updated
    self.short_name = short_name
    self.readme_text = readme_text
    self.project_visibility = project_visibility
    self.workspace_id = workspace_id
    self.workspace_type = workspace_type
    self.attributes = attributes
    self.tags = tags
    self.owner = owner
    self.code_version_snapshot = code_version_snapshot
    self.artifacts = artifacts

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    
    
    
    from .ModeldbProjectVisibility import ModeldbProjectVisibility

    
    from .WorkspaceTypeEnumWorkspaceType import WorkspaceTypeEnumWorkspaceType

    from .CommonKeyValue import CommonKeyValue

    
    
    from .ModeldbCodeVersion import ModeldbCodeVersion

    from .ModeldbArtifact import ModeldbArtifact


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('description', None)
    if tmp is not None:
      d['description'] = tmp
    tmp = d.get('date_created', None)
    if tmp is not None:
      d['date_created'] = tmp
    tmp = d.get('date_updated', None)
    if tmp is not None:
      d['date_updated'] = tmp
    tmp = d.get('short_name', None)
    if tmp is not None:
      d['short_name'] = tmp
    tmp = d.get('readme_text', None)
    if tmp is not None:
      d['readme_text'] = tmp
    tmp = d.get('project_visibility', None)
    if tmp is not None:
      d['project_visibility'] = ModeldbProjectVisibility.from_json(tmp)
    tmp = d.get('workspace_id', None)
    if tmp is not None:
      d['workspace_id'] = tmp
    tmp = d.get('workspace_type', None)
    if tmp is not None:
      d['workspace_type'] = WorkspaceTypeEnumWorkspaceType.from_json(tmp)
    tmp = d.get('attributes', None)
    if tmp is not None:
      d['attributes'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]
    tmp = d.get('tags', None)
    if tmp is not None:
      d['tags'] = [tmp for tmp in tmp]
    tmp = d.get('owner', None)
    if tmp is not None:
      d['owner'] = tmp
    tmp = d.get('code_version_snapshot', None)
    if tmp is not None:
      d['code_version_snapshot'] = ModeldbCodeVersion.from_json(tmp)
    tmp = d.get('artifacts', None)
    if tmp is not None:
      d['artifacts'] = [ModeldbArtifact.from_json(tmp) for tmp in tmp]

    return ModeldbProject(**d)
