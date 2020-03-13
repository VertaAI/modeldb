# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindExperiments(BaseType):
  def __init__(self, project_id=None, experiment_ids=None, predicates=None, ids_only=None, page_number=None, page_limit=None, ascending=None, sort_key=None):
    required = {
      "project_id": False,
      "experiment_ids": False,
      "predicates": False,
      "ids_only": False,
      "page_number": False,
      "page_limit": False,
      "ascending": False,
      "sort_key": False,
    }
    self.project_id = project_id
    self.experiment_ids = experiment_ids
    self.predicates = predicates
    self.ids_only = ids_only
    self.page_number = page_number
    self.page_limit = page_limit
    self.ascending = ascending
    self.sort_key = sort_key

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    from .ModeldbKeyValueQuery import ModeldbKeyValueQuery

    
    
    
    
    

    tmp = d.get('project_id', None)
    if tmp is not None:
      d['project_id'] = tmp
    tmp = d.get('experiment_ids', None)
    if tmp is not None:
      d['experiment_ids'] = [tmp for tmp in tmp]
    tmp = d.get('predicates', None)
    if tmp is not None:
      d['predicates'] = [ModeldbKeyValueQuery.from_json(tmp) for tmp in tmp]
    tmp = d.get('ids_only', None)
    if tmp is not None:
      d['ids_only'] = tmp
    tmp = d.get('page_number', None)
    if tmp is not None:
      d['page_number'] = tmp
    tmp = d.get('page_limit', None)
    if tmp is not None:
      d['page_limit'] = tmp
    tmp = d.get('ascending', None)
    if tmp is not None:
      d['ascending'] = tmp
    tmp = d.get('sort_key', None)
    if tmp is not None:
      d['sort_key'] = tmp

    return ModeldbFindExperiments(**d)
