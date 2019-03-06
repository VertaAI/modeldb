export interface IHyperparameter {
  key: string;
  value: number | string;
}

export class Hyperparameter implements IHyperparameter {
  public readonly key: string;
  public readonly value: number | string;
  constructor(key: string, value: number | string) {
    this.key = key;
    this.value = value;
  }
}
