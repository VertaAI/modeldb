export interface IDataset {
  key: string;
  path: string;
  type: string;
}

export class Dataset implements IDataset {
  public readonly key: string;
  public readonly path: string;
  public readonly type: string;
  constructor(key: string, path: string, type: string) {
    this.key = key;
    this.path = path;
    this.type = type;
  }
}
