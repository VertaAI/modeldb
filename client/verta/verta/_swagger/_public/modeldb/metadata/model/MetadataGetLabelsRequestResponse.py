# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class MetadataGetLabelsRequestResponse(BaseType):
  def __init__(self, labels=None):
    required = {
      "labels": False,
    }
    self.labels = labels

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('labels', None)
    if tmp is not None:
      d['labels'] = [tmp for tmp in tmp]

    return MetadataGetLabelsRequestResponse(**d)
