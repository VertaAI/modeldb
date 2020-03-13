# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetHydratedExperimentRunByIdResponse(BaseType):
  def __init__(self, hydrated_experiment_run=None):
    required = {
      "hydrated_experiment_run": False,
    }
    self.hydrated_experiment_run = hydrated_experiment_run

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbHydratedExperimentRun import ModeldbHydratedExperimentRun


    tmp = d.get('hydrated_experiment_run', None)
    if tmp is not None:
      d['hydrated_experiment_run'] = ModeldbHydratedExperimentRun.from_json(tmp)

    return ModeldbGetHydratedExperimentRunByIdResponse(**d)
