# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class CanaryRule(BaseType):
  def __init__(self, description=None, id=None, name=None, rule_parameters=None):
    required = {
      "description": False,
      "id": False,
      "name": False,
      "rule_parameters": False,
    }
    self.description = description
    self.id = id
    self.name = name
    self.rule_parameters = rule_parameters

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    

    tmp = d.get('description', None)
    if tmp is not None:
      d['description'] = tmp
    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('rule_parameters', None)
    if tmp is not None:
      d['rule_parameters'] = [ { "name": (lambda tmp: tmp)(tmp.get("name")), "description": (lambda tmp: tmp)(tmp.get("description")),  }  for tmp in tmp]

    return CanaryRule(**d)
