# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbQueryDatasetVersionInfo(BaseType):
  def __init__(self, query=None, query_template=None, query_parameters=None, data_source_uri=None, execution_timestamp=None, num_records=None):
    required = {
      "query": False,
      "query_template": False,
      "query_parameters": False,
      "data_source_uri": False,
      "execution_timestamp": False,
      "num_records": False,
    }
    self.query = query
    self.query_template = query_template
    self.query_parameters = query_parameters
    self.data_source_uri = data_source_uri
    self.execution_timestamp = execution_timestamp
    self.num_records = num_records

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    from .ModeldbQueryParameter import ModeldbQueryParameter

    
    
    

    tmp = d.get('query', None)
    if tmp is not None:
      d['query'] = tmp
    tmp = d.get('query_template', None)
    if tmp is not None:
      d['query_template'] = tmp
    tmp = d.get('query_parameters', None)
    if tmp is not None:
      d['query_parameters'] = [ModeldbQueryParameter.from_json(tmp) for tmp in tmp]
    tmp = d.get('data_source_uri', None)
    if tmp is not None:
      d['data_source_uri'] = tmp
    tmp = d.get('execution_timestamp', None)
    if tmp is not None:
      d['execution_timestamp'] = tmp
    tmp = d.get('num_records', None)
    if tmp is not None:
      d['num_records'] = tmp

    return ModeldbQueryDatasetVersionInfo(**d)
