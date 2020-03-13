# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbLogMetric(BaseType):
  def __init__(self, id=None, metric=None):
    required = {
      "id": False,
      "metric": False,
    }
    self.id = id
    self.metric = metric

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .CommonKeyValue import CommonKeyValue


    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('metric', None)
    if tmp is not None:
      d['metric'] = CommonKeyValue.from_json(tmp)

    return ModeldbLogMetric(**d)
