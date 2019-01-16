export enum ModelType {
  LinearRegression = 'LinearRegression'
}

export class Model {
  private id: string = '';
  private modelType: ModelType = ModelType.LinearRegression;

  public get Id(): string {
    return this.id;
  }

  public set Id(v: string) {
    this.id = v;
  }
}
