# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetJobIdResponse(BaseType):
  def __init__(self, job_id=None):
    required = {
      "job_id": False,
    }
    self.job_id = job_id

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    

    tmp = d.get('job_id', None)
    if tmp is not None:
      d['job_id'] = tmp

    return ModeldbGetJobIdResponse(**d)
