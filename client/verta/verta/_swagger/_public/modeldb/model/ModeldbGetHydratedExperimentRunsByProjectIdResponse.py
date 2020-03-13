# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetHydratedExperimentRunsByProjectIdResponse(BaseType):
  def __init__(self, hydrated_experiment_runs=None, total_records=None):
    required = {
      "hydrated_experiment_runs": False,
      "total_records": False,
    }
    self.hydrated_experiment_runs = hydrated_experiment_runs
    self.total_records = total_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbHydratedExperimentRun import ModeldbHydratedExperimentRun

    

    tmp = d.get('hydrated_experiment_runs', None)
    if tmp is not None:
      d['hydrated_experiment_runs'] = [ModeldbHydratedExperimentRun.from_json(tmp) for tmp in tmp]
    tmp = d.get('total_records', None)
    if tmp is not None:
      d['total_records'] = tmp

    return ModeldbGetHydratedExperimentRunsByProjectIdResponse(**d)
