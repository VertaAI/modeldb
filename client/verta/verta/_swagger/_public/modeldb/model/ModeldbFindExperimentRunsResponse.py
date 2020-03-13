# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindExperimentRunsResponse(BaseType):
  def __init__(self, experiment_runs=None, total_records=None):
    required = {
      "experiment_runs": False,
      "total_records": False,
    }
    self.experiment_runs = experiment_runs
    self.total_records = total_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbExperimentRun import ModeldbExperimentRun

    

    tmp = d.get('experiment_runs', None)
    if tmp is not None:
      d['experiment_runs'] = [ModeldbExperimentRun.from_json(tmp) for tmp in tmp]
    tmp = d.get('total_records', None)
    if tmp is not None:
      d['total_records'] = tmp

    return ModeldbFindExperimentRunsResponse(**d)
