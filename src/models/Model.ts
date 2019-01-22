import { Artifact, IArtifact } from './Artifact';

export enum ModelType {
  LinearRegression = 'LinearRegression'
}

export class Model {
  private id: string = '';
  private projectId: string = '';
  private experimentId: string = '';
  private dataFrameId: string = '';
  private name: string = '';

  private tags: string[] = Array<string>();
  private hyperparameters: Map<string, string> = new Map<string, string>();
  private modelMetrics: Map<string, string> = new Map<string, string>();
  private artifacts: IArtifact[] = Array<Artifact>();
  private dataSets: IArtifact[] = Array<Artifact>();
  private timestamp: Date = new Date();
  private modelType: ModelType = ModelType.LinearRegression;

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

  public get DataFrameId(): string {
    return this.dataFrameId;
  }
  public set DataFrameId(v: string) {
    this.dataFrameId = v;
  }

  public get ModelType(): ModelType {
    return this.modelType;
  }
  public set ModelType(v: ModelType) {
    this.modelType = v;
  }

  public get Hyperparameters(): Map<string, string> {
    return this.hyperparameters;
  }
  public set Hyperparameters(v: Map<string, string>) {
    this.hyperparameters = v;
  }

  public get ModelMetric(): Map<string, string> {
    return this.modelMetrics;
  }
  public set ModelMetric(v: Map<string, string>) {
    this.modelMetrics = v;
  }

  public get Artifacts(): IArtifact[] {
    return this.artifacts;
  }
  public set Artifacts(v: IArtifact[]) {
    this.artifacts = v;
  }

  public get DataSets(): IArtifact[] {
    return this.dataSets;
  }
  public set DataSets(v: IArtifact[]) {
    this.dataSets = v;
  }

  public get Timestamp(): Date {
    return this.timestamp;
  }
  public set Timestamp(v: Date) {
    this.timestamp = v;
  }
}
