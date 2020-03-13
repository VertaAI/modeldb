# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class UacCollectTelemetry(BaseType):
  def __init__(self, id=None, metrics=None):
    required = {
      "id": False,
      "metrics": False,
    }
    self.id = id
    self.metrics = metrics

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .CommonKeyValue import CommonKeyValue


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('metrics', None)
    if tmp is not None:
      d['metrics'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]

    return UacCollectTelemetry(**d)
