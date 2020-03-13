# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbCreateDatasetVersion(BaseType):
  def __init__(self, dataset_id=None, parent_id=None, description=None, tags=None, dataset_version_visibility=None, dataset_type=None, attributes=None, version=None, raw_dataset_version_info=None, path_dataset_version_info=None, query_dataset_version_info=None, time_created=None):
    required = {
      "dataset_id": False,
      "parent_id": False,
      "description": False,
      "tags": False,
      "dataset_version_visibility": False,
      "dataset_type": False,
      "attributes": False,
      "version": False,
      "raw_dataset_version_info": False,
      "path_dataset_version_info": False,
      "query_dataset_version_info": False,
      "time_created": False,
    }
    self.dataset_id = dataset_id
    self.parent_id = parent_id
    self.description = description
    self.tags = tags
    self.dataset_version_visibility = dataset_version_visibility
    self.dataset_type = dataset_type
    self.attributes = attributes
    self.version = version
    self.raw_dataset_version_info = raw_dataset_version_info
    self.path_dataset_version_info = path_dataset_version_info
    self.query_dataset_version_info = query_dataset_version_info
    self.time_created = time_created

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    from .DatasetVisibilityEnumDatasetVisibility import DatasetVisibilityEnumDatasetVisibility

    from .DatasetTypeEnumDatasetType import DatasetTypeEnumDatasetType

    from .CommonKeyValue import CommonKeyValue

    
    from .ModeldbRawDatasetVersionInfo import ModeldbRawDatasetVersionInfo

    from .ModeldbPathDatasetVersionInfo import ModeldbPathDatasetVersionInfo

    from .ModeldbQueryDatasetVersionInfo import ModeldbQueryDatasetVersionInfo

    

    tmp = d.get('dataset_id', None)
    if tmp is not None:
      d['dataset_id'] = tmp
    tmp = d.get('parent_id', None)
    if tmp is not None:
      d['parent_id'] = tmp
    tmp = d.get('description', None)
    if tmp is not None:
      d['description'] = tmp
    tmp = d.get('tags', None)
    if tmp is not None:
      d['tags'] = [tmp for tmp in tmp]
    tmp = d.get('dataset_version_visibility', None)
    if tmp is not None:
      d['dataset_version_visibility'] = DatasetVisibilityEnumDatasetVisibility.from_json(tmp)
    tmp = d.get('dataset_type', None)
    if tmp is not None:
      d['dataset_type'] = DatasetTypeEnumDatasetType.from_json(tmp)
    tmp = d.get('attributes', None)
    if tmp is not None:
      d['attributes'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]
    tmp = d.get('version', None)
    if tmp is not None:
      d['version'] = tmp
    tmp = d.get('raw_dataset_version_info', None)
    if tmp is not None:
      d['raw_dataset_version_info'] = ModeldbRawDatasetVersionInfo.from_json(tmp)
    tmp = d.get('path_dataset_version_info', None)
    if tmp is not None:
      d['path_dataset_version_info'] = ModeldbPathDatasetVersionInfo.from_json(tmp)
    tmp = d.get('query_dataset_version_info', None)
    if tmp is not None:
      d['query_dataset_version_info'] = ModeldbQueryDatasetVersionInfo.from_json(tmp)
    tmp = d.get('time_created', None)
    if tmp is not None:
      d['time_created'] = tmp

    return ModeldbCreateDatasetVersion(**d)
