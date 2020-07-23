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
      d['latency'] = 
    {
            "average": (lambda tmp: list(map(lambda tmp: 
tmp
, (tmp or [])))

)(tmp.get("average")),
            "p90": (lambda tmp: list(map(lambda tmp: 
tmp
, (tmp or [])))

)(tmp.get("p90")),
            "p99": (lambda tmp: list(map(lambda tmp: 
tmp
, (tmp or [])))

)(tmp.get("p99")),
    }


    tmp = d.get('throughput', None)
    if tmp is not None:
      d['throughput'] = 
    dict(zip(
        tmp.keys()
        map(
            lambda tmp: list(map(lambda tmp: 
tmp
, (tmp or [])))

, tmp.items()
        )
    ))


    tmp = d.get('time', None)
    if tmp is not None:
      d['time'] = list(map(lambda tmp: 
tmp
, (tmp or [])))



    return StageBuildMetrics(**d)
