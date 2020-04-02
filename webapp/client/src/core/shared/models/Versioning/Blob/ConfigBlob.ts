import { IElementDiff, IBlobDiff } from './Diff';

export interface IConfigBlob {
  category: 'config';
  data: {
    hyperparameterSet: IConfigHyperparameterSetItem[];
    hyperparameters: IConfigHyperparameter[];
  };
}

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

export type IConfigHyperparameterDiff = IElementDiff<IConfigHyperparameter>;
export type IConfigHyperparameterSetItemDiff = IElementDiff<
  IConfigHyperparameterSetItem
>;

export type IConfigBlobDiff = IBlobDiff<
  IConfigBlobDiffData,
  IConfigBlob['category']
>;

export type IConfigBlobDiffData = {
  hyperparameters?: IConfigHyperparameterDiff[];
  hyperparameterSet?: IConfigHyperparameterSetItemDiff[];
};
