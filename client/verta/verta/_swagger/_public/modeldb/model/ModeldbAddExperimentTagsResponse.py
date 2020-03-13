# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbAddExperimentTagsResponse(BaseType):
  def __init__(self, experiment=None):
    required = {
      "experiment": False,
    }
    self.experiment = experiment

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbExperiment import ModeldbExperiment


    tmp = d.get('experiment', None)
    if tmp is not None:
      d['experiment'] = ModeldbExperiment.from_json(tmp)

    return ModeldbAddExperimentTagsResponse(**d)
