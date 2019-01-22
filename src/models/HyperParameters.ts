export interface IHyperparameter {
  key: string;
  value: string;
}

export class Hyperparameter implements IHyperparameter {
  public readonly key: string;
  public readonly value: string;
  constructor(key: string, value: string) {
    this.key = key;
    this.value = value;
  }
}
