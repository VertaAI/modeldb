# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindExperimentsResponse(BaseType):
  def __init__(self, experiments=None, total_records=None):
    required = {
      "experiments": False,
      "total_records": False,
    }
    self.experiments = experiments
    self.total_records = total_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbExperiment import ModeldbExperiment

    

    tmp = d.get('experiments', None)
    if tmp is not None:
      d['experiments'] = [ModeldbExperiment.from_json(tmp) for tmp in tmp]
    tmp = d.get('total_records', None)
    if tmp is not None:
      d['total_records'] = tmp

    return ModeldbFindExperimentsResponse(**d)
