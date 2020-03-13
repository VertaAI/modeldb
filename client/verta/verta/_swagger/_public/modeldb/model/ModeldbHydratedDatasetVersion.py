# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbHydratedDatasetVersion(BaseType):
  def __init__(self, dataset_version=None, owner_user_info=None, allowed_actions=None):
    required = {
      "dataset_version": False,
      "owner_user_info": False,
      "allowed_actions": False,
    }
    self.dataset_version = dataset_version
    self.owner_user_info = owner_user_info
    self.allowed_actions = allowed_actions

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbDatasetVersion import ModeldbDatasetVersion

    from .UacUserInfo import UacUserInfo

    from .UacAction import UacAction


    tmp = d.get('dataset_version', None)
    if tmp is not None:
      d['dataset_version'] = ModeldbDatasetVersion.from_json(tmp)
    tmp = d.get('owner_user_info', None)
    if tmp is not None:
      d['owner_user_info'] = UacUserInfo.from_json(tmp)
    tmp = d.get('allowed_actions', None)
    if tmp is not None:
      d['allowed_actions'] = [UacAction.from_json(tmp) for tmp in tmp]

    return ModeldbHydratedDatasetVersion(**d)
