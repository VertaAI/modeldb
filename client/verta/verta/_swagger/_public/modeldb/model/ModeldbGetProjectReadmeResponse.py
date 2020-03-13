# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetProjectReadmeResponse(BaseType):
  def __init__(self, readme_text=None):
    required = {
      "readme_text": False,
    }
    self.readme_text = readme_text

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('readme_text', None)
    if tmp is not None:
      d['readme_text'] = tmp

    return ModeldbGetProjectReadmeResponse(**d)
