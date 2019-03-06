import { JsonObject, JsonProperty } from 'json2typescript';

import { StringToDateConverter } from '../utils/MapperConverters';
import { Artifact, IArtifact } from './Artifact';
import { Dataset, IDataset } from './Dataset';
import { PropertyType } from './Filters';
import { Hyperparameter, IHyperparameter } from './HyperParameters';
import { IMetaData } from './IMetaData';
import { IMetric, Metric } from './Metrics';
import { IObservation, Observation } from './Observation';

@JsonObject('modelRecord')
export default class ModelRecord {
  public static metaData: IMetaData[] = [
    { propertyName: 'Name', type: PropertyType.STRING },
    { propertyName: 'ID', type: PropertyType.NUMBER },
    { propertyName: 'Experiment ID', type: PropertyType.NUMBER },
    { propertyName: 'Experiment Run ID', type: PropertyType.NUMBER }
  ];

  @JsonProperty('id', String, true)
  public id: string = '';
  @JsonProperty('project_id', String, true)
  public projectId: string = '';
  @JsonProperty('experiment_id', String, true)
  public experimentId: string = '';
  @JsonProperty('name', String, true)
  public name: string = '';
  @JsonProperty('code_version', String, true)
  public codeVersion: string = '';
  @JsonProperty('description', String, true)
  public description: string = '';
  @JsonProperty('owner', String, true)
  public owner: string = '';
  @JsonProperty('date_created', StringToDateConverter, true)
  public dateCreated: Date = new Date();
  @JsonProperty('date_updated', StringToDateConverter, true)
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
  @JsonProperty('artifacts', [Artifact], true)
  public artifacts: IArtifact[] = [];
  @JsonProperty('datasets', [Dataset], true)
  public datasets: IDataset[] = [];
  @JsonProperty('observations', [Observation], true)
  public observations: IObservation[] = [];
}
