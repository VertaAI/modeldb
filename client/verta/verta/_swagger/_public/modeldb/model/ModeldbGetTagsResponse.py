# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetTagsResponse(BaseType):
  def __init__(self, tags=None):
    required = {
      "tags": False,
    }
    self.tags = tags

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('tags', None)
    if tmp is not None:
      d['tags'] = [tmp for tmp in tmp]

    return ModeldbGetTagsResponse(**d)
