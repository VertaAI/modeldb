# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class StageUpdateRequest(BaseType):
  def __init__(self, build_id=None, canary_strategy=None, strategy=None):
    required = {
      "build_id": False,
      "canary_strategy": False,
      "strategy": False,
    }
    self.build_id = build_id
    self.canary_strategy = canary_strategy
    self.strategy = strategy

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .CanaryStrategy import CanaryStrategy

    

    tmp = d.get('build_id', None)
    if tmp is not None:
      d['build_id'] = tmp
    tmp = d.get('canary_strategy', None)
    if tmp is not None:
      d['canary_strategy'] = CanaryStrategy.from_json(tmp)
    tmp = d.get('strategy', None)
    if tmp is not None:
      d['strategy'] = tmp

    return StageUpdateRequest(**d)
