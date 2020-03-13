# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbHydratedExperimentRun(BaseType):
  def __init__(self, experiment_run=None, comments=None, owner_user_info=None, experiment=None, allowed_actions=None):
    required = {
      "experiment_run": False,
      "comments": False,
      "owner_user_info": False,
      "experiment": False,
      "allowed_actions": False,
    }
    self.experiment_run = experiment_run
    self.comments = comments
    self.owner_user_info = owner_user_info
    self.experiment = experiment
    self.allowed_actions = allowed_actions

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbExperimentRun import ModeldbExperimentRun

    from .ModeldbComment import ModeldbComment

    from .UacUserInfo import UacUserInfo

    from .ModeldbExperiment import ModeldbExperiment

    from .UacAction import UacAction


    tmp = d.get('experiment_run', None)
    if tmp is not None:
      d['experiment_run'] = ModeldbExperimentRun.from_json(tmp)
    tmp = d.get('comments', None)
    if tmp is not None:
      d['comments'] = [ModeldbComment.from_json(tmp) for tmp in tmp]
    tmp = d.get('owner_user_info', None)
    if tmp is not None:
      d['owner_user_info'] = UacUserInfo.from_json(tmp)
    tmp = d.get('experiment', None)
    if tmp is not None:
      d['experiment'] = ModeldbExperiment.from_json(tmp)
    tmp = d.get('allowed_actions', None)
    if tmp is not None:
      d['allowed_actions'] = [UacAction.from_json(tmp) for tmp in tmp]

    return ModeldbHydratedExperimentRun(**d)
