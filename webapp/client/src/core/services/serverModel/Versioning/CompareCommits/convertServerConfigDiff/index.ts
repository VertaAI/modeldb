import { IConfigBlobDiff, IConfigBlobDiffData } from 'core/shared/models/Versioning/Blob/ConfigBlob';
import {
  convertCongifHyperparameter,
  convertConfigHyperparameterSetItem,
} from '../../RepositoryData/Blob/ConfigBlob';
import { IServerBlobDiff, IServerElementDiff, convertServerElementDiffToClient } from '../ServerDiff';

export const convertServerConfigDiff = (serverDiff: IServerConfigHyperparameterDiff): IConfigBlobDiff => {
  const serverConfig = serverDiff.config;
  switch (serverDiff.status) {
    case 'ADDED': {
      return {
        category: 'config',
        type: 'config',
        diffType: 'added',
        data: convertServerConfigDiffData(serverConfig),
        location: serverDiff.location as any,
      };
    }
    case 'DELETED': {
      return {
        category: 'config',
        type: 'config',
        diffType: 'deleted',
        data: convertServerConfigDiffData(serverConfig),
        location: serverDiff.location as any,
      };
    }
    case 'MODIFIED': {
      return {
        category: 'config',
        type: 'config',
        diffType: 'updated',
        data: convertServerConfigDiffData(serverConfig),
        location: serverDiff.location as any,
      };
    }
  }
};

export const convertServerConfigDiffData = (serverConfig: IServerConfigHyperparameterDiff['config']): IConfigBlobDiffData => {
  return {
    hyperparameterSet: serverConfig.hyperparameter_set && serverConfig.hyperparameter_set.map((serverHypSetItem) => {
      return convertServerElementDiffToClient(convertConfigHyperparameterSetItem, serverHypSetItem);
    }),
    hyperparameters: serverConfig.hyperparameters && serverConfig.hyperparameters.map((serverHyp) => {
      return convertServerElementDiffToClient(convertCongifHyperparameter, serverHyp);
    })
  };
};

export type IServerConfigHyperparameterDiff = IServerBlobDiff<{
  config: {
    hyperparameters?: Array<IServerElementDiff<{
      name: string;
      value: { int_value: number } | { float_value: number } | { string_value: string };
    }>>;
    hyperparameter_set?: Array<IServerElementDiff<{
      name: string;
      value: {
        continuous: {
          interval_begin: { int_value: number } | { float_value: number } | { string_value: string };
          interval_end: { int_value: number } | { float_value: number } | { string_value: string };
          interval_step: { int_value: number } | { float_value: number } | { string_value: string };
        };
        discrete: {
          values: Array<{ int_value: number } | { float_value: number } | { string_value: string }>;
        };
      };
    }>>;
  }
}>;
