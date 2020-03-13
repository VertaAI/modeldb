# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class MetadataIdentificationType(BaseType):
  def __init__(self, id_type=None, int_id=None, string_id=None):
    required = {
      "id_type": False,
      "int_id": False,
      "string_id": False,
    }
    self.id_type = id_type
    self.int_id = int_id
    self.string_id = string_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .IDTypeEnumIDType import IDTypeEnumIDType

    
    

    tmp = d.get('id_type', None)
    if tmp is not None:
      d['id_type'] = IDTypeEnumIDType.from_json(tmp)
    tmp = d.get('int_id', None)
    if tmp is not None:
      d['int_id'] = tmp
    tmp = d.get('string_id', None)
    if tmp is not None:
      d['string_id'] = tmp

    return MetadataIdentificationType(**d)
