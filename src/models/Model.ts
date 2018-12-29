import { BaseEntity } from "./BaseEntity";

export enum ModelType {
    LinearRegression = 'LinearRegression',
}

export class Model extends BaseEntity {
  private type: ModelType = ModelType.LinearRegression;

  public get Type(): ModelType {
    return this.type;
  }

  public set Type(v: ModelType) {
    this.type = v;
  }

}
