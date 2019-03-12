import { JsonObject, JsonProperty } from 'json2typescript';

export interface IDataset {
  key: string;
  path: string;
  type: string;
}

@JsonObject('artifact')
export class Dataset implements IDataset {
  @JsonProperty('key', String)
  public readonly key: string;
  @JsonProperty('path', String)
  public readonly path: string;
  @JsonProperty('artifact_type', String, true)
  public readonly type: string;

  constructor(key?: string, path?: string, type?: string) {
    this.key = key || '';
    this.path = path || '';
    this.type = type || '';
  }
}
