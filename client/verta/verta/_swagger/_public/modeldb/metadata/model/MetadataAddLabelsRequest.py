# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class MetadataAddLabelsRequest(BaseType):
  def __init__(self, id=None, labels=None):
    required = {
      "id": False,
      "labels": False,
    }
    self.id = id
    self.labels = labels

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .MetadataIdentificationType import MetadataIdentificationType

    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = MetadataIdentificationType.from_json(tmp)
    tmp = d.get('labels', None)
    if tmp is not None:
      d['labels'] = [tmp for tmp in tmp]

    return MetadataAddLabelsRequest(**d)
