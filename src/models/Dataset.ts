export interface IDataset {
  key: string;
  path: string;
  artifactType: string;
}

export class Dataset implements IDataset {
  public readonly key: string;
  public readonly path: string;
  public readonly artifactType: string;
  constructor(key: string, path: string, artifactType: string) {
    this.key = key;
    this.path = path;
    this.artifactType = artifactType;
  }
}
