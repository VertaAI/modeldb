# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbCreateExperiment(BaseType):
  def __init__(self, project_id=None, name=None, description=None, date_created=None, date_updated=None, attributes=None, tags=None, artifacts=None):
    required = {
      "project_id": False,
      "name": False,
      "description": False,
      "date_created": False,
      "date_updated": False,
      "attributes": False,
      "tags": False,
      "artifacts": False,
    }
    self.project_id = project_id
    self.name = name
    self.description = description
    self.date_created = date_created
    self.date_updated = date_updated
    self.attributes = attributes
    self.tags = tags
    self.artifacts = artifacts

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    
    from .CommonKeyValue import CommonKeyValue

    
    from .ModeldbArtifact import ModeldbArtifact


    tmp = d.get('project_id', None)
    if tmp is not None:
      d['project_id'] = tmp
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
    tmp = d.get('attributes', None)
    if tmp is not None:
      d['attributes'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]
    tmp = d.get('tags', None)
    if tmp is not None:
      d['tags'] = [tmp for tmp in tmp]
    tmp = d.get('artifacts', None)
    if tmp is not None:
      d['artifacts'] = [ModeldbArtifact.from_json(tmp) for tmp in tmp]

    return ModeldbCreateExperiment(**d)
