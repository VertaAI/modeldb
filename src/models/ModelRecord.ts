import { IArtifact } from './Artifact';
import { IHyperparameter } from './HyperParameters';
import { IMetric } from './Metrics';

export default class ModelRecord {
  private id: string = '';
  private projectId: string = '';
  private experimentId: string = '';
  private name: string = '';
  private codeVersion: string = '';

  private tags: string[] = [];
  private hyperparameters: IHyperparameter[] = [];
  private metrics: IMetric[] = [];
  private artifacts: IArtifact[] = [];

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

  public get Metric(): IMetric[] {
    return this.metrics;
  }
  public set Metric(v: IMetric[]) {
    this.metrics = v;
  }

  public get Artifacts(): IArtifact[] {
    return this.artifacts;
  }
  public set Artifacts(v: IArtifact[]) {
    this.artifacts = v;
  }
}
