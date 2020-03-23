import { ICodeBlob } from './CodeBlob';
import { IConfigBlob } from './ConfigBlob';
import { IDatasetBlob } from './DatasetBlob';
import { IEnvironmentBlob } from './EnvironmentBlob';

export interface IUnknowBlob {
  category: 'unknown';
  data: object;
}

export interface IBlob {
  type: 'blob';
  data: IDatasetBlob | ICodeBlob | IEnvironmentBlob | IConfigBlob | IUnknowBlob;
}

export const blobCategories: Record<
  IBlob['data']['category'],
  IBlob['data']['category']
> = {
  dataset: 'dataset',
  unknown: 'unknown',
  code: 'code',
  environment: 'environment',
  config: 'config',
};
