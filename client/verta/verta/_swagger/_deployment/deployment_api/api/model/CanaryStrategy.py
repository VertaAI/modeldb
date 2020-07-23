# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class CanaryStrategy(BaseType):
  def __init__(self, progress_interval_seconds=None, progress_step=None, rules=None):
    required = {
      "progress_interval_seconds": False,
      "progress_step": False,
      "rules": False,
    }
    self.progress_interval_seconds = progress_interval_seconds
    self.progress_step = progress_step
    self.rules = rules

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    

    tmp = d.get('progress_interval_seconds', None)
    if tmp is not None:
      d['progress_interval_seconds'] = 
tmp

    tmp = d.get('progress_step', None)
    if tmp is not None:
      d['progress_step'] = 
tmp

    tmp = d.get('rules', None)
    if tmp is not None:
      d['rules'] = list(map(lambda tmp: 
    {
            "rule_id": (lambda tmp: 
tmp
)(tmp.get("rule_id")),
            "rule_parameters": (lambda tmp: list(map(lambda tmp: 
    {
            "name": (lambda tmp: 
tmp
)(tmp.get("name")),
            "value": (lambda tmp: 
tmp
)(tmp.get("value")),
    }

, (tmp or [])))

)(tmp.get("rule_parameters")),
    }

, (tmp or [])))



    return CanaryStrategy(**d)
