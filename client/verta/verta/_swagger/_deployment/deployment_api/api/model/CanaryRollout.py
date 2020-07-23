# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class CanaryRollout(BaseType):
  def __init__(self, rollout=None, time=None):
    required = {
      "rollout": False,
      "time": False,
    }
    self.rollout = rollout
    self.time = time

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('rollout', None)
    if tmp is not None:
      d['rollout'] = list(map(lambda tmp: 
    {
            "build_id": (lambda tmp: 
tmp
)(tmp.get("build_id")),
            "ratio": (lambda tmp: list(map(lambda tmp: 
tmp
, (tmp or [])))

)(tmp.get("ratio")),
    }

, (tmp or [])))


    tmp = d.get('time', None)
    if tmp is not None:
      d['time'] = list(map(lambda tmp: 
tmp
, (tmp or [])))



    return CanaryRollout(**d)
