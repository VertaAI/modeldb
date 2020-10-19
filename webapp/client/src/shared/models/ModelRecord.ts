import { JsonObject, JsonProperty } from 'json2typescript';

import { StringToDateConverter } from 'shared/utils/mapperConverters';

import { Artifact, IArtifact } from 'shared/models/Artifact';
import { IAttribute, Attribute } from 'shared/models/Attribute';
import { ICodeVersion } from 'shared/models/CodeVersion';
import * as Common from 'shared/models/Common';
import { Hyperparameter, IHyperparameter } from 'shared/models/HyperParameters';
import { IMetric, Metric } from 'shared/models/Metrics';
import { IObservation, Observation } from 'shared/models/Observation';
import { CommitComponentLocation } from 'shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'shared/models/Versioning/Repository';
import { ICommit } from 'shared/models/Versioning/RepositoryData';

import * as EntitiesActions from './EntitiesActions';
import Experiment, { ShortExperiment } from './Experiment';
import User from './User';
import { Project } from './Project';

export type BlobLocation = string;
export type ICodeVersionsFromBlob = Record<BlobLocation, ICodeVersion>;

@JsonObject('modelRecord')
export default class ModelRecord
  implements
    Common.IEntityWithLogging,
    EntitiesActions.IEntityWithAllowedActions {
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
  public codeVersionsFromBlob?: ICodeVersionsFromBlob = undefined;
  @JsonProperty('description', String, true)
  public description: string = '';
  @JsonProperty('owner', String, true)
  public ownerId: string = '';
  public owner: User = new User({ id: '', email: '', username: '' });
  public dateCreated: Date = new Date();
  public dateUpdated: Date = new Date();
  @JsonProperty('start_time', StringToDateConverter, true)
  public startTime: Date = new Date();
  @JsonProperty('end_time', StringToDateConverter, true)
  public endTime: Date = new Date();

  public allowedActions: EntitiesActions.IEntityWithAllowedActions['allowedActions'] = [];

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

  public versionedInputs?: IVersionedInputs;
}

export interface IVersionedInputs {
  repositoryId: IRepository['id'];
  repositoryName: IRepository['name'];
  commitSha: ICommit['sha'];
  keyLocationMap: {
    [K: string]: {
      location: CommitComponentLocation;
    };
  };
}

export type LoadExperimentRunErrorType = Common.EntityErrorType;

export type IExperimentRunInfo = Pick<ModelRecord, 'name' | 'id'> & {
  experiment: Pick<Experiment, 'name' | 'id'>;
  project: Pick<Project, 'name' | 'id'>;
};
