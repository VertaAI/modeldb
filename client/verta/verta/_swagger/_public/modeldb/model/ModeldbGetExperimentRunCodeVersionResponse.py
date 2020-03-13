# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetExperimentRunCodeVersionResponse(BaseType):
  def __init__(self, code_version=None):
    required = {
      "code_version": False,
    }
    self.code_version = code_version

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbCodeVersion import ModeldbCodeVersion


    tmp = d.get('code_version', None)
    if tmp is not None:
      d['code_version'] = ModeldbCodeVersion.from_json(tmp)

    return ModeldbGetExperimentRunCodeVersionResponse(**d)
