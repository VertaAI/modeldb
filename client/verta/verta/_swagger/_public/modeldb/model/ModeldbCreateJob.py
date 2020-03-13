# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbCreateJob(BaseType):
  def __init__(self, description=None, start_time=None, end_time=None, metadata=None, job_status=None, job_type=None):
    required = {
      "description": False,
      "start_time": False,
      "end_time": False,
      "metadata": False,
      "job_status": False,
      "job_type": False,
    }
    self.description = description
    self.start_time = start_time
    self.end_time = end_time
    self.metadata = metadata
    self.job_status = job_status
    self.job_type = job_type

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    from .CommonKeyValue import CommonKeyValue

    from .JobStatusEnumJobStatus import JobStatusEnumJobStatus

    from .JobTypeEnumJobType import JobTypeEnumJobType


    tmp = d.get('description', None)
    if tmp is not None:
      d['description'] = tmp
    tmp = d.get('start_time', None)
    if tmp is not None:
      d['start_time'] = tmp
    tmp = d.get('end_time', None)
    if tmp is not None:
      d['end_time'] = tmp
    tmp = d.get('metadata', None)
    if tmp is not None:
      d['metadata'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]
    tmp = d.get('job_status', None)
    if tmp is not None:
      d['job_status'] = JobStatusEnumJobStatus.from_json(tmp)
    tmp = d.get('job_type', None)
    if tmp is not None:
      d['job_type'] = JobTypeEnumJobType.from_json(tmp)

    return ModeldbCreateJob(**d)
