import { JsonObject, JsonProperty } from 'json2typescript';

import { StringToDateConverter } from 'core/shared/utils/mapperConverters';

import { Artifact, IArtifact } from 'core/shared/models/Artifact';
import { IAttribute, Attribute } from 'core/shared/models/Attribute';
import { ICodeVersion } from 'core/shared/models/CodeVersion';
import * as Common from 'core/shared/models/Common';
import {
  Hyperparameter,
  IHyperparameter,
} from 'core/shared/models/HyperParameters';
import { IMetric, Metric } from 'core/shared/models/Metrics';
import { IObservation, Observation } from 'core/shared/models/Observation';
import { ShortExperiment } from './Experiment';

@JsonObject('modelRecord')
export default class ModelRecord implements Common.IEntityWithLogging {
  @JsonProperty('id', String, true)
  public id: string = '';
  @JsonProperty('project_id', String, true)
  public projectId: string = '';
  @JsonProperty('experiment_id', String, true)
  public experimentId: string = '';
  @JsonProperty('experiment', ShortExperiment, true)
  public shortExperiment: ShortExperiment = undefined as any;
  @JsonProperty('name', String, true)
  public name: string = '';
  public codeVersion?: ICodeVersion = undefined;
  @JsonProperty('description', String, true)
  public description: string = '';
  public dateCreated: Date = new Date();
  public dateUpdated: Date = new Date();
  @JsonProperty('start_time', StringToDateConverter, true)
  public startTime: Date = new Date();
  @JsonProperty('end_time', StringToDateConverter, true)
  public endTime: Date = new Date();

  @JsonProperty('tags', [String], true)
  public tags: string[] = [];
  @JsonProperty('hyperparameters', [Hyperparameter], true)
  public hyperparameters: IHyperparameter[] = [];
  @JsonProperty('metrics', [Metric], true)
  public metrics: IMetric[] = [];
  @JsonProperty('datasets', [Artifact], true)
  public datasets: IArtifact[] = [];
  @JsonProperty('artifacts', [Artifact], true)
  public artifacts: IArtifact[] = [];
  @JsonProperty('observations', [Observation], true)
  public observations: IObservation[] = [];
  @JsonProperty('attributes', [Attribute], true)
  public attributes: IAttribute[] = [];
}

export type LoadExperimentRunErrorType = Common.EntityErrorType;
