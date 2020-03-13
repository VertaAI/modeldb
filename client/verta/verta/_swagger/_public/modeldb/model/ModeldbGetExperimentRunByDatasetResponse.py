# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetExperimentRunByDatasetResponse(BaseType):
  def __init__(self, experiment_runs=None):
    required = {
      "experiment_runs": False,
    }
    self.experiment_runs = experiment_runs

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbExperimentRun import ModeldbExperimentRun


    tmp = d.get('experiment_runs', None)
    if tmp is not None:
      d['experiment_runs'] = [ModeldbExperimentRun.from_json(tmp) for tmp in tmp]

    return ModeldbGetExperimentRunByDatasetResponse(**d)
