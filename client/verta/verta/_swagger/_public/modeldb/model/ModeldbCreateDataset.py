# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbCreateDataset(BaseType):
  def __init__(self, name=None, description=None, tags=None, attributes=None, dataset_visibility=None, dataset_type=None, workspace_name=None, time_created=None):
    required = {
      "name": False,
      "description": False,
      "tags": False,
      "attributes": False,
      "dataset_visibility": False,
      "dataset_type": False,
      "workspace_name": False,
      "time_created": False,
    }
    self.name = name
    self.description = description
    self.tags = tags
    self.attributes = attributes
    self.dataset_visibility = dataset_visibility
    self.dataset_type = dataset_type
    self.workspace_name = workspace_name
    self.time_created = time_created

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    from .CommonKeyValue import CommonKeyValue

    from .DatasetVisibilityEnumDatasetVisibility import DatasetVisibilityEnumDatasetVisibility

    from .DatasetTypeEnumDatasetType import DatasetTypeEnumDatasetType

    
    

    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('description', None)
    if tmp is not None:
      d['description'] = tmp
    tmp = d.get('tags', None)
    if tmp is not None:
      d['tags'] = [tmp for tmp in tmp]
    tmp = d.get('attributes', None)
    if tmp is not None:
      d['attributes'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]
    tmp = d.get('dataset_visibility', None)
    if tmp is not None:
      d['dataset_visibility'] = DatasetVisibilityEnumDatasetVisibility.from_json(tmp)
    tmp = d.get('dataset_type', None)
    if tmp is not None:
      d['dataset_type'] = DatasetTypeEnumDatasetType.from_json(tmp)
    tmp = d.get('workspace_name', None)
    if tmp is not None:
      d['workspace_name'] = tmp
    tmp = d.get('time_created', None)
    if tmp is not None:
      d['time_created'] = tmp

    return ModeldbCreateDataset(**d)
