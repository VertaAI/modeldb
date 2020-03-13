# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbCreateProject(BaseType):
  def __init__(self, name=None, description=None, attributes=None, tags=None, readme_text=None, project_visibility=None, artifacts=None, workspace_name=None, date_created=None):
    required = {
      "name": False,
      "description": False,
      "attributes": False,
      "tags": False,
      "readme_text": False,
      "project_visibility": False,
      "artifacts": False,
      "workspace_name": False,
      "date_created": False,
    }
    self.name = name
    self.description = description
    self.attributes = attributes
    self.tags = tags
    self.readme_text = readme_text
    self.project_visibility = project_visibility
    self.artifacts = artifacts
    self.workspace_name = workspace_name
    self.date_created = date_created

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    from .CommonKeyValue import CommonKeyValue

    
    
    from .ModeldbProjectVisibility import ModeldbProjectVisibility

    from .ModeldbArtifact import ModeldbArtifact

    
    

    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('description', None)
    if tmp is not None:
      d['description'] = tmp
    tmp = d.get('attributes', None)
    if tmp is not None:
      d['attributes'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]
    tmp = d.get('tags', None)
    if tmp is not None:
      d['tags'] = [tmp for tmp in tmp]
    tmp = d.get('readme_text', None)
    if tmp is not None:
      d['readme_text'] = tmp
    tmp = d.get('project_visibility', None)
    if tmp is not None:
      d['project_visibility'] = ModeldbProjectVisibility.from_json(tmp)
    tmp = d.get('artifacts', None)
    if tmp is not None:
      d['artifacts'] = [ModeldbArtifact.from_json(tmp) for tmp in tmp]
    tmp = d.get('workspace_name', None)
    if tmp is not None:
      d['workspace_name'] = tmp
    tmp = d.get('date_created', None)
    if tmp is not None:
      d['date_created'] = tmp

    return ModeldbCreateProject(**d)
