# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbExperimentRun(BaseType):
  def __init__(self, id=None, project_id=None, experiment_id=None, name=None, description=None, date_created=None, date_updated=None, start_time=None, end_time=None, code_version=None, code_version_snapshot=None, parent_id=None, tags=None, attributes=None, hyperparameters=None, artifacts=None, datasets=None, metrics=None, observations=None, features=None, job_id=None, owner=None):
    required = {
      "id": False,
      "project_id": False,
      "experiment_id": False,
      "name": False,
      "description": False,
      "date_created": False,
      "date_updated": False,
      "start_time": False,
      "end_time": False,
      "code_version": False,
      "code_version_snapshot": False,
      "parent_id": False,
      "tags": False,
      "attributes": False,
      "hyperparameters": False,
      "artifacts": False,
      "datasets": False,
      "metrics": False,
      "observations": False,
      "features": False,
      "job_id": False,
      "owner": False,
    }
    self.id = id
    self.project_id = project_id
    self.experiment_id = experiment_id
    self.name = name
    self.description = description
    self.date_created = date_created
    self.date_updated = date_updated
    self.start_time = start_time
    self.end_time = end_time
    self.code_version = code_version
    self.code_version_snapshot = code_version_snapshot
    self.parent_id = parent_id
    self.tags = tags
    self.attributes = attributes
    self.hyperparameters = hyperparameters
    self.artifacts = artifacts
    self.datasets = datasets
    self.metrics = metrics
    self.observations = observations
    self.features = features
    self.job_id = job_id
    self.owner = owner

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    
    
    
    
    
    
    from .ModeldbCodeVersion import ModeldbCodeVersion

    
    
    from .CommonKeyValue import CommonKeyValue

    from .CommonKeyValue import CommonKeyValue

    from .ModeldbArtifact import ModeldbArtifact

    from .ModeldbArtifact import ModeldbArtifact

    from .CommonKeyValue import CommonKeyValue

    from .ModeldbObservation import ModeldbObservation

    from .ModeldbFeature import ModeldbFeature

    
    

    tmp = d.get('id', None)
    if tmp is not None:
      d['id'] = tmp
    tmp = d.get('project_id', None)
    if tmp is not None:
      d['project_id'] = tmp
    tmp = d.get('experiment_id', None)
    if tmp is not None:
      d['experiment_id'] = tmp
    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('description', None)
    if tmp is not None:
      d['description'] = tmp
    tmp = d.get('date_created', None)
    if tmp is not None:
      d['date_created'] = tmp
    tmp = d.get('date_updated', None)
    if tmp is not None:
      d['date_updated'] = tmp
    tmp = d.get('start_time', None)
    if tmp is not None:
      d['start_time'] = tmp
    tmp = d.get('end_time', None)
    if tmp is not None:
      d['end_time'] = tmp
    tmp = d.get('code_version', None)
    if tmp is not None:
      d['code_version'] = tmp
    tmp = d.get('code_version_snapshot', None)
    if tmp is not None:
      d['code_version_snapshot'] = ModeldbCodeVersion.from_json(tmp)
    tmp = d.get('parent_id', None)
    if tmp is not None:
      d['parent_id'] = tmp
    tmp = d.get('tags', None)
    if tmp is not None:
      d['tags'] = [tmp for tmp in tmp]
    tmp = d.get('attributes', None)
    if tmp is not None:
      d['attributes'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]
    tmp = d.get('hyperparameters', None)
    if tmp is not None:
      d['hyperparameters'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]
    tmp = d.get('artifacts', None)
    if tmp is not None:
      d['artifacts'] = [ModeldbArtifact.from_json(tmp) for tmp in tmp]
    tmp = d.get('datasets', None)
    if tmp is not None:
      d['datasets'] = [ModeldbArtifact.from_json(tmp) for tmp in tmp]
    tmp = d.get('metrics', None)
    if tmp is not None:
      d['metrics'] = [CommonKeyValue.from_json(tmp) for tmp in tmp]
    tmp = d.get('observations', None)
    if tmp is not None:
      d['observations'] = [ModeldbObservation.from_json(tmp) for tmp in tmp]
    tmp = d.get('features', None)
    if tmp is not None:
      d['features'] = [ModeldbFeature.from_json(tmp) for tmp in tmp]
    tmp = d.get('job_id', None)
    if tmp is not None:
      d['job_id'] = tmp
    tmp = d.get('owner', None)
    if tmp is not None:
      d['owner'] = tmp

    return ModeldbExperimentRun(**d)
