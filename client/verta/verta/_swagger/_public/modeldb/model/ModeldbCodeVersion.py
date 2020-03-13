# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbCodeVersion(BaseType):
  def __init__(self, git_snapshot=None, code_archive=None, date_logged=None):
    required = {
      "git_snapshot": False,
      "code_archive": False,
      "date_logged": False,
    }
    self.git_snapshot = git_snapshot
    self.code_archive = code_archive
    self.date_logged = date_logged

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    from .ModeldbGitSnapshot import ModeldbGitSnapshot

    from .ModeldbArtifact import ModeldbArtifact

    

    tmp = d.get('git_snapshot', None)
    if tmp is not None:
      d['git_snapshot'] = ModeldbGitSnapshot.from_json(tmp)
    tmp = d.get('code_archive', None)
    if tmp is not None:
      d['code_archive'] = ModeldbArtifact.from_json(tmp)
    tmp = d.get('date_logged', None)
    if tmp is not None:
      d['date_logged'] = tmp

    return ModeldbCodeVersion(**d)
