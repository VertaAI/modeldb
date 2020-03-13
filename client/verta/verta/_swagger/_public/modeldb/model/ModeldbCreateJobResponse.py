# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbCreateJobResponse(BaseType):
  def __init__(self, job=None):
    required = {
      "job": False,
    }
    self.job = job

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbJob import ModeldbJob


    tmp = d.get('job', None)
    if tmp is not None:
      d['job'] = ModeldbJob.from_json(tmp)

    return ModeldbCreateJobResponse(**d)
