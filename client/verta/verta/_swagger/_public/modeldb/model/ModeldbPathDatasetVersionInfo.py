# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbPathDatasetVersionInfo(BaseType):
  def __init__(self, location_type=None, size=None, dataset_part_infos=None, base_path=None):
    required = {
      "location_type": False,
      "size": False,
      "dataset_part_infos": False,
      "base_path": False,
    }
    self.location_type = location_type
    self.size = size
    self.dataset_part_infos = dataset_part_infos
    self.base_path = base_path

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .PathLocationTypeEnumPathLocationType import PathLocationTypeEnumPathLocationType

    
    from .ModeldbDatasetPartInfo import ModeldbDatasetPartInfo

    

    tmp = d.get('location_type', None)
    if tmp is not None:
      d['location_type'] = PathLocationTypeEnumPathLocationType.from_json(tmp)
    tmp = d.get('size', None)
    if tmp is not None:
      d['size'] = tmp
    tmp = d.get('dataset_part_infos', None)
    if tmp is not None:
      d['dataset_part_infos'] = [ModeldbDatasetPartInfo.from_json(tmp) for tmp in tmp]
    tmp = d.get('base_path', None)
    if tmp is not None:
      d['base_path'] = tmp

    return ModeldbPathDatasetVersionInfo(**d)
