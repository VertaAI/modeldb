# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class StageStatus(BaseType):
  def __init__(self, traffic_shape=None, update_request=None):
    required = {
      "traffic_shape": False,
      "update_request": False,
    }
    self.traffic_shape = traffic_shape
    self.update_request = update_request

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    from .StageUpdateRequest import StageUpdateRequest


    tmp = d.get('traffic_shape', None)
    if tmp is not None:
      d['traffic_shape'] =  { "ratio": (lambda tmp: [ { "build_id": (lambda tmp: tmp)(tmp.get("build_id")), "value": (lambda tmp: [tmp for tmp in tmp])(tmp.get("value")),  }  for tmp in tmp])(tmp.get("ratio")), "time": (lambda tmp: [tmp for tmp in tmp])(tmp.get("time")),  } 
    tmp = d.get('update_request', None)
    if tmp is not None:
      d['update_request'] = StageUpdateRequest.from_json(tmp)

    return StageStatus(**d)
