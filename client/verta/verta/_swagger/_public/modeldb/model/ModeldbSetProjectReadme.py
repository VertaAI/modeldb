# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbSetProjectReadme(BaseType):
  def __init__(self, id=None, readme_text=None):
    required = {
      "id": False,
      "readme_text": False,
    }
    self.id = id
    self.readme_text = readme_text

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('readme_text', None)
    if tmp is not None:
      d['readme_text'] = tmp

    return ModeldbSetProjectReadme(**d)
