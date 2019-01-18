export enum ModelType {
  LinearRegression = 'LinearRegression'
}

export class Model {
  private id: string = '';
  private dataFrameId: string = '';
  private modelMetric: Map<string, string> = new Map<string, string>();
  private timestamp: Date = new Date();
  private modelType: ModelType = ModelType.LinearRegression;

  public get Id(): string {
    return this.id;
  }
  public set Id(v: string) {
    this.id = v;
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

  public get ModelMetric(): Map<string, string> {
    return this.modelMetric;
  }
  public set ModelMetric(v: Map<string, string>) {
    this.modelMetric = v;
  }

  public get Timestamp(): Date {
    return this.timestamp;
  }
  public set Timestamp(v: Date) {
    this.timestamp = v;
  }
}
