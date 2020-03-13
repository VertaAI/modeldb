# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbFindDatasets(BaseType):
  def __init__(self, dataset_ids=None, predicates=None, ids_only=None, workspace_name=None, page_number=None, page_limit=None, ascending=None, sort_key=None):
    required = {
      "dataset_ids": False,
      "predicates": False,
      "ids_only": False,
      "workspace_name": False,
      "page_number": False,
      "page_limit": False,
      "ascending": False,
      "sort_key": False,
    }
    self.dataset_ids = dataset_ids
    self.predicates = predicates
    self.ids_only = ids_only
    self.workspace_name = workspace_name
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

    
    
    
    
    
    

    tmp = d.get('dataset_ids', None)
    if tmp is not None:
      d['dataset_ids'] = [tmp for tmp in tmp]
    tmp = d.get('predicates', None)
    if tmp is not None:
      d['predicates'] = [ModeldbKeyValueQuery.from_json(tmp) for tmp in tmp]
    tmp = d.get('ids_only', None)
    if tmp is not None:
      d['ids_only'] = tmp
    tmp = d.get('workspace_name', None)
    if tmp is not None:
      d['workspace_name'] = tmp
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

    return ModeldbFindDatasets(**d)
