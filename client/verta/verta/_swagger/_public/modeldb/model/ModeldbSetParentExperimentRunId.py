# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbSetParentExperimentRunId(BaseType):
  def __init__(self, experiment_run_id=None, parent_id=None):
    required = {
      "experiment_run_id": False,
      "parent_id": False,
    }
    self.experiment_run_id = experiment_run_id
    self.parent_id = parent_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    

    tmp = d.get('experiment_run_id', None)
    if tmp is not None:
      d['experiment_run_id'] = tmp
    tmp = d.get('parent_id', None)
    if tmp is not None:
      d['parent_id'] = tmp

    return ModeldbSetParentExperimentRunId(**d)
