# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetUrlForArtifactResponse(BaseType):
  def __init__(self, url=None, fields=None):
    required = {
      "url": False,
      "fields": False,
    }
    self.url = url
    self.fields = fields

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('url', None)
    if tmp is not None:
      d['url'] = tmp
    tmp = d.get('fields', None)
    if tmp is not None:
      d['fields'] = {k: tmp for k, tmp in tmp.items()}

    return ModeldbGetUrlForArtifactResponse(**d)
