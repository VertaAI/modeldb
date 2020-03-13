# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetHydratedExperimentsByProjectIdResponse(BaseType):
  def __init__(self, hydrated_experiments=None, total_records=None):
    required = {
      "hydrated_experiments": False,
      "total_records": False,
    }
    self.hydrated_experiments = hydrated_experiments
    self.total_records = total_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbHydratedExperiment import ModeldbHydratedExperiment

    

    tmp = d.get('hydrated_experiments', None)
    if tmp is not None:
      d['hydrated_experiments'] = [ModeldbHydratedExperiment.from_json(tmp) for tmp in tmp]
    tmp = d.get('total_records', None)
    if tmp is not None:
      d['total_records'] = tmp

    return ModeldbGetHydratedExperimentsByProjectIdResponse(**d)
