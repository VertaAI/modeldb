import { IArtifact } from './Artifact';
import { IDataset } from './Dataset';
import { PropertyType } from './Filters';
import { IHyperparameter } from './HyperParameters';
import { IMetaData } from './IMetaData';
import { IMetric } from './Metrics';
import { IObservation } from './Observation';

export default class ModelRecord {
  public static metaData: IMetaData[] = [
    { propertyName: 'Name', type: PropertyType.STRING },
    { propertyName: 'ID', type: PropertyType.NUMBER },
    { propertyName: 'Experiment ID', type: PropertyType.NUMBER },
    { propertyName: 'Experiment Run ID', type: PropertyType.NUMBER }
  ];

  private id: string = '';
  private projectId: string = '';
  private experimentId: string = '';
  private name: string = '';
  private codeVersion: string = '';
  private description: string = '';
  private owner: string = '';
  private dateCreated: Date = new Date();
  private dateUpdated: Date = new Date();
  private startTime: Date = new Date();
  private endTime: Date = new Date();

  private tags: string[] = [];
  private hyperparameters: IHyperparameter[] = [];
  private metrics: IMetric[] = [];
  private artifacts: IArtifact[] = [];
  private datasets: IDataset[] = [];
  private observations: IObservation[] = [];

  public get Id(): string {
    return this.id;
  }
  public set Id(v: string) {
    this.id = v;
  }

  public get Name(): string {
    return this.name;
  }
  public set Name(v: string) {
    this.name = v;
  }

  public get CodeVersion(): string {
    return this.codeVersion;
  }
  public set CodeVersion(v: string) {
    this.codeVersion = v;
  }

  public get Description(): string {
    return this.description;
  }
  public set Description(v: string) {
    this.description = v;
  }

  public get Owner(): string {
    return this.owner;
  }
  public set Owner(v: string) {
    this.owner = v;
  }

  public get DateCreated(): Date {
    return this.dateCreated;
  }

  public set DateCreated(v: Date) {
    this.dateCreated = v;
  }

  public get DateUpdated(): Date {
    return this.dateUpdated;
  }

  public set DateUpdated(v: Date) {
    this.dateUpdated = v;
  }

  public get StartTime(): Date {
    return this.startTime;
  }

  public set StartTime(v: Date) {
    this.startTime = v;
  }

  public get EndTime(): Date {
    return this.endTime;
  }

  public set EndTime(v: Date) {
    this.endTime = v;
  }

  public get ProjectId(): string {
    return this.projectId;
  }
  public set ProjectId(v: string) {
    this.projectId = v;
  }

  public get ExperimentId(): string {
    return this.experimentId;
  }
  public set ExperimentId(v: string) {
    this.experimentId = v;
  }

  public get Tags(): string[] {
    return this.tags;
  }
  public set Tags(v: string[]) {
    this.tags = v;
  }

  public get Hyperparameters(): IHyperparameter[] {
    return this.hyperparameters;
  }
  public set Hyperparameters(v: IHyperparameter[]) {
    this.hyperparameters = v;
  }

  public get Metrics(): IMetric[] {
    return this.metrics;
  }
  public set Metrics(v: IMetric[]) {
    this.metrics = v;
  }

  public get Artifacts(): IArtifact[] {
    return this.artifacts;
  }
  public set Artifacts(v: IArtifact[]) {
    this.artifacts = v;
  }

  public get Datasets(): IDataset[] {
    return this.datasets;
  }
  public set Datasets(v: IDataset[]) {
    this.datasets = v;
  }

  public get Observations(): IObservation[] {
    return this.observations;
  }
  public set Observations(v: IObservation[]) {
    this.observations = v;
  }
}
