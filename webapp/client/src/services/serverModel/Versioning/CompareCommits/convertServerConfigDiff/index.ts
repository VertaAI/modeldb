import {
  IConfigBlobDiff,
  IConfigBlobDiffData,
} from 'shared/models/Versioning/Blob/ConfigBlob';
import {
  convertCongifHyperparameter,
  convertConfigHyperparameterSetItem,
} from '../../RepositoryData/Blob/ConfigBlob';
import {
  IServerBlobDiff,
  IServerElementDiff,
  convertServerBlobDiffToClient,
  convertNullableServerArrayDiffToClient,
} from '../ServerDiff';

export const convertServerConfigDiff = (
  serverDiff: IServerConfigHyperparameterDiff
): IConfigBlobDiff => {
  return convertServerBlobDiffToClient<
    IServerConfigHyperparameterDiff,
    IConfigBlobDiff
  >(
    {
      convertData: ({ config }) => {
        const res: IConfigBlobDiffData = {
          hyperparameterSet: convertNullableServerArrayDiffToClient(
            convertConfigHyperparameterSetItem,
            config.hyperparameter_set
          ),
          hyperparameters: convertNullableServerArrayDiffToClient(
            convertCongifHyperparameter,
            config.hyperparameters
          ),
        };
        return res;
      },
      category: 'config',
      type: 'config',
    },
    serverDiff
  );
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
