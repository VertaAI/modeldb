import {
  IConfigBlobDiff,
  IConfigBlobDiffData,
} from 'core/shared/models/Versioning/Blob/ConfigBlob';
import {
  convertCongifHyperparameter,
  convertConfigHyperparameterSetItem,
} from '../../RepositoryData/Blob/ConfigBlob';
import {
  IServerBlobDiff,
  IServerElementDiff,
  convertServerElementDiffToClient,
  convertServerBlobDiffToClient,
} from '../ServerDiff';

export const convertServerConfigDiff = (
  serverDiff: IServerConfigHyperparameterDiff
): IConfigBlobDiff => {
  return convertServerBlobDiffToClient<
    IServerConfigHyperparameterDiff,
    IConfigBlobDiff
  >(
    {
      convertData: ({ config }) => convertServerConfigDiffData(config),
      category: 'config',
      type: 'config',
    },
    serverDiff
  );
};

export const convertServerConfigDiffData = (
  serverConfig: IServerConfigHyperparameterDiff['config']
): IConfigBlobDiffData => {
  return {
    hyperparameterSet:
      serverConfig.hyperparameter_set &&
      serverConfig.hyperparameter_set.map(serverHypSetItem => {
        return convertServerElementDiffToClient(
          convertConfigHyperparameterSetItem,
          serverHypSetItem
        );
      }),
    hyperparameters:
      serverConfig.hyperparameters &&
      serverConfig.hyperparameters.map(serverHyp => {
        return convertServerElementDiffToClient(
          convertCongifHyperparameter,
          serverHyp
        );
      }),
  };
};

export type IServerConfigHyperparameterDiff = IServerBlobDiff<{
  config: {
    hyperparameters?: Array<
      IServerElementDiff<{
        name: string;
        value:
          | { int_value: number }
          | { float_value: number }
          | { string_value: string };
      }>
    >;
    hyperparameter_set?: Array<
      IServerElementDiff<{
        name: string;
        value: {
          continuous: {
            interval_begin:
              | { int_value: number }
              | { float_value: number }
              | { string_value: string };
            interval_end:
              | { int_value: number }
              | { float_value: number }
              | { string_value: string };
            interval_step:
              | { int_value: number }
              | { float_value: number }
              | { string_value: string };
          };
          discrete: {
            values: Array<
              | { int_value: number }
              | { float_value: number }
              | { string_value: string }
            >;
          };
        };
      }>
    >;
  };
}>;
