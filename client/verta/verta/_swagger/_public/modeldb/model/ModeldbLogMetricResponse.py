# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbLogMetricResponse(BaseType):
  def __init__(self, experiment_run=None):
    required = {
      "experiment_run": False,
    }
    self.experiment_run = experiment_run

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbExperimentRun import ModeldbExperimentRun


    tmp = d.get('experiment_run', None)
    if tmp is not None:
      d['experiment_run'] = ModeldbExperimentRun.from_json(tmp)

    return ModeldbLogMetricResponse(**d)
