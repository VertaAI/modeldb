import { GenericDiff } from './Diff';

export interface IConfigBlob {
  category: 'config';
  data: {
    hyperparameterSet: IConfigHyperparameterSetItem[];
    hyperparameters: IConfigHyperparameter[];
  };
}
export type IConfigBlobDiff = GenericDiff<
  IConfigBlobDataDiff,
  IConfigBlob['category'],
  IConfigBlob['category']
>;
export type IConfigBlobDataDiff = Omit<IConfigBlob, 'data'> & {
  data: Partial<IConfigBlob['data']>;
};

export interface IConfigHyperparameter {
  name: string;
  value: IConfigHyperparameterValue;
}

export type IConfigHyperparameterSetItem =
  | IDiscreteConfigHyperparameterSetItem
  | IContinuousHyperparameterSetItem;

export interface IDiscreteConfigHyperparameterSetItem {
  type: 'discrete';
  name: string;
  values: IConfigHyperparameterValue[];
}

export interface IContinuousHyperparameterSetItem {
  type: 'continuous';
  name: string;
  intervalBegin: IConfigHyperparameterValue;
  intervalEnd: IConfigHyperparameterValue;
  intervalStep: IConfigHyperparameterValue;
}

export type IConfigHyperparameterValue =
  | { type: 'int'; value: number }
  | { type: 'float'; value: number }
  | { type: 'string'; value: string };
