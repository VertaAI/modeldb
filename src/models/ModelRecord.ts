import { IArtifact } from './Artifact';
import { IHyperparameter } from './HyperParameters';
import { IModelMetric } from './ModelMetric';

export class ModelRecord {
  private id: string = '';
  private projectId: string = '';
  private projectName: string = '';
  private experimentId: string = '';
  private name: string = '';
  private description: string = '';
  private dateCreated?: Date = undefined;
  private dateUpdated?: Date = undefined;
  private codeVersion: string = '';
  private version: string = '';

  private tags: string[] = [];
  private hyperparameters: IHyperparameter[] = [];
  private modelMetrics: IModelMetric[] = [];
  private artifacts: IArtifact[] = [];
  private dataSets: IArtifact[] = [];

  public get Id(): string {
    return this.id;
  }
  public set Id(v: string) {
    this.id = v;
  }

  public get Version(): string {
    return this.version;
  }
  public set Version(v: string) {
    this.version = v;
  }

  public get Name(): string {
    return this.name;
  }
  public set Name(v: string) {
    this.name = v;
  }

  public get Description(): string {
    return this.description;
  }
  public set Description(v: string) {
    this.description = v;
  }

  public get CodeVersion(): string {
    return this.codeVersion;
  }
  public set CodeVersion(v: string) {
    this.codeVersion = v;
  }

  public get ProjectId(): string {
    return this.projectId;
  }
  public set ProjectId(v: string) {
    this.projectId = v;
  }

  public get ProjectName(): string {
    return this.projectName;
  }
  public set ProjectName(v: string) {
    this.projectName = v;
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

  public get ModelMetric(): IModelMetric[] {
    return this.modelMetrics;
  }
  public set ModelMetric(v: IModelMetric[]) {
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

  public get DateCreated(): Date | undefined {
    return this.dateCreated;
  }
  public set DateCreated(v: Date | undefined) {
    this.dateCreated = v;
  }

  public get DateUpdated(): Date | undefined {
    return this.dateUpdated;
  }
  public set DateUpdated(v: Date | undefined) {
    this.dateUpdated = v;
  }
}
