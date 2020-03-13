# THIS FILE IS AUTO-GENERATED. DO NOT EDIT
from verta._swagger.base_type import BaseType

class ModeldbGetSummaryResponse(BaseType):
  def __init__(self, name=None, last_updated_time=None, total_experiment=None, total_experiment_runs=None, last_modified_experimentRun_summary=None, metrics=None):
    required = {
      "name": False,
      "last_updated_time": False,
      "total_experiment": False,
      "total_experiment_runs": False,
      "last_modified_experimentRun_summary": False,
      "metrics": False,
    }
    self.name = name
    self.last_updated_time = last_updated_time
    self.total_experiment = total_experiment
    self.total_experiment_runs = total_experiment_runs
    self.last_modified_experimentRun_summary = last_modified_experimentRun_summary
    self.metrics = metrics

    for k, v in required.items():
      if self[k] is None and v:
        raise ValueError('attribute {} is required'.format(k))

  @staticmethod
  def from_json(d):
    
    
    
    
    from .ModeldbLastModifiedExperimentRunSummary import ModeldbLastModifiedExperimentRunSummary

    from .ModeldbMetricsSummary import ModeldbMetricsSummary


    tmp = d.get('name', None)
    if tmp is not None:
      d['name'] = tmp
    tmp = d.get('last_updated_time', None)
    if tmp is not None:
      d['last_updated_time'] = tmp
    tmp = d.get('total_experiment', None)
    if tmp is not None:
      d['total_experiment'] = tmp
    tmp = d.get('total_experiment_runs', None)
    if tmp is not None:
      d['total_experiment_runs'] = tmp
    tmp = d.get('last_modified_experimentRun_summary', None)
    if tmp is not None:
      d['last_modified_experimentRun_summary'] = ModeldbLastModifiedExperimentRunSummary.from_json(tmp)
    tmp = d.get('metrics', None)
    if tmp is not None:
      d['metrics'] = [ModeldbMetricsSummary.from_json(tmp) for tmp in tmp]

    return ModeldbGetSummaryResponse(**d)
