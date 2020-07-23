# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class StageBuildMetrics(BaseType):
  def __init__(self, latency=None, throughput=None, time=None):
    required = {
      "latency": False,
      "throughput": False,
      "time": False,
    }
    self.latency = latency
    self.throughput = throughput
    self.time = time

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    

    tmp = d.get('latency', None)
    if tmp is not None:
      d['latency'] =  { "average": (lambda tmp: [tmp for tmp in tmp])(tmp.get("average")), "p50": (lambda tmp: [tmp for tmp in tmp])(tmp.get("p50")), "p90": (lambda tmp: [tmp for tmp in tmp])(tmp.get("p90")), "p99": (lambda tmp: [tmp for tmp in tmp])(tmp.get("p99")),  } 
    tmp = d.get('throughput', None)
    if tmp is not None:
      d['throughput'] = {k: [tmp for tmp in tmp] for k, tmp in tmp.items()}
    tmp = d.get('time', None)
    if tmp is not None:
      d['time'] = [tmp for tmp in tmp]

    return StageBuildMetrics(**d)
