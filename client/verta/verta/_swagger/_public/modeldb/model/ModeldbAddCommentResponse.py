# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbAddCommentResponse(BaseType):
  def __init__(self, comment=None):
    required = {
      "comment": False,
    }
    self.comment = comment

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbComment import ModeldbComment


    tmp = d.get('comment', None)
    if tmp is not None:
      d['comment'] = ModeldbComment.from_json(tmp)

    return ModeldbAddCommentResponse(**d)
