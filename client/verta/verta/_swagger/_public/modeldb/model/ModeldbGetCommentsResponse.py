# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetCommentsResponse(BaseType):
  def __init__(self, comments=None):
    required = {
      "comments": False,
    }
    self.comments = comments

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbComment import ModeldbComment


    tmp = d.get('comments', None)
    if tmp is not None:
      d['comments'] = [ModeldbComment.from_json(tmp) for tmp in tmp]

    return ModeldbGetCommentsResponse(**d)
