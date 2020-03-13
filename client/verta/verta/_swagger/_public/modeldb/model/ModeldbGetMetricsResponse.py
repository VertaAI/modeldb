# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetMetricsResponse(BaseType):
  def __init__(self, metrics=None):
    required = {
      "metrics": False,
    }
    self.metrics = metrics

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .CommonKeyValue import CommonKeyValue


    tmp = d.get('metrics', None)
    if tmp is not None:
      d['metrics'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]

    return ModeldbGetMetricsResponse(**d)
