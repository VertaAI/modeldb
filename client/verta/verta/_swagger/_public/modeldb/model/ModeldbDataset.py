# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbDataset(BaseType):
  def __init__(self, id=None, name=None, owner=None, description=None, tags=None, dataset_visibility=None, dataset_type=None, attributes=None, time_created=None, time_updated=None, workspace_id=None, workspace_type=None):
    required = {
      "id": False,
      "name": False,
      "owner": False,
      "description": False,
      "tags": False,
      "dataset_visibility": False,
      "dataset_type": False,
      "attributes": False,
      "time_created": False,
      "time_updated": False,
      "workspace_id": False,
      "workspace_type": False,
    }
    self.id = id
    self.name = name
    self.owner = owner
    self.description = description
    self.tags = tags
    self.dataset_visibility = dataset_visibility
    self.dataset_type = dataset_type
    self.attributes = attributes
    self.time_created = time_created
    self.time_updated = time_updated
    self.workspace_id = workspace_id
    self.workspace_type = workspace_type

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    
    from .DatasetVisibilityEnumDatasetVisibility import DatasetVisibilityEnumDatasetVisibility

    from .DatasetTypeEnumDatasetType import DatasetTypeEnumDatasetType

    from .CommonKeyValue import CommonKeyValue

    
    
    
    from .WorkspaceTypeEnumWorkspaceType import WorkspaceTypeEnumWorkspaceType


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('owner', None)
    if tmp is not None:
      d['owner'] = tmp
    tmp = d.get('description', None)
    if tmp is not None:
      d['description'] = tmp
    tmp = d.get('tags', None)
    if tmp is not None:
      d['tags'] = [tmp for tmp in tmp]
    tmp = d.get('dataset_visibility', None)
    if tmp is not None:
      d['dataset_visibility'] = DatasetVisibilityEnumDatasetVisibility.from_json(tmp)
    tmp = d.get('dataset_type', None)
    if tmp is not None:
      d['dataset_type'] = DatasetTypeEnumDatasetType.from_json(tmp)
    tmp = d.get('attributes', None)
    if tmp is not None:
      d['attributes'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]
    tmp = d.get('time_created', None)
    if tmp is not None:
      d['time_created'] = tmp
    tmp = d.get('time_updated', None)
    if tmp is not None:
      d['time_updated'] = tmp
    tmp = d.get('workspace_id', None)
    if tmp is not None:
      d['workspace_id'] = tmp
    tmp = d.get('workspace_type', None)
    if tmp is not None:
      d['workspace_type'] = WorkspaceTypeEnumWorkspaceType.from_json(tmp)

    return ModeldbDataset(**d)
