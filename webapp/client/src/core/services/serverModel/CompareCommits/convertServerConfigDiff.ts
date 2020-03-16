import * as R from 'ramda';

import { IConfigBlobDiff } from 'core/shared/models/Repository/Blob/ConfigBlob';
import { DiffType } from 'core/shared/models/Repository/Blob/Diff';
import {
  convertServerHyperparameterSet,
  convertServerHyperparameters,
} from '../RepositoryData/Blob/ConfigBlob';

export const convertServerConfigDiff = (
  serverConfigDiff: any,
  diffType: DiffType
): IConfigBlobDiff => {
  const { location, config } = serverConfigDiff;

  switch (diffType) {
    case 'added':
    case 'deleted': {
      return {
        type: 'config',
        category: 'config',
        diffType,
        blob: {
          category: 'config',
          data: {
            hyperparameters:
              config.hyperparameters && !R.isEmpty(config.hyperparameters)
                ? convertServerHyperparameters(
                    config.hyperparameters.B || config.hyperparameters.A
                  )
                : undefined,
            hyperparameterSet:
              config.hyperparameter_set && !R.isEmpty(config.hyperparameter_set)
                ? convertServerHyperparameterSet(
                    config.hyperparameter_set.B || config.hyperparameter_set.A
                  )
                : undefined,
          },
        },
        location,
      };
    }

    case 'updated': {
      return {
        type: 'config',
        category: 'config',
        diffType: 'updated',
        location,
        blobA: {
          category: 'config',
          data: {
            hyperparameters:
              config.hyperparameters && !R.isEmpty(config.hyperparameters)
                ? convertServerHyperparameters(config.hyperparameters.A)
                : undefined,
            hyperparameterSet:
              config.hyperparameter_set && !R.isEmpty(config.hyperparameter_set)
                ? convertServerHyperparameterSet(config.hyperparameter_set.A)
                : undefined,
          },
        },
        blobB: {
          category: 'config',
          data: {
            hyperparameters:
              config.hyperparameters && !R.isEmpty(config.hyperparameters)
                ? convertServerHyperparameters(config.hyperparameters.B)
                : undefined,
            hyperparameterSet:
              config.hyperparameter_set && !R.isEmpty(config.hyperparameter_set)
                ? convertServerHyperparameterSet(config.hyperparameter_set.B)
                : undefined,
          },
        },
      };
    }

    default:
      throw new Error('is not handled!');
  }
};
