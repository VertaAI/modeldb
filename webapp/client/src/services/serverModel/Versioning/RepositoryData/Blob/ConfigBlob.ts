import {
  IConfigBlob,
  IConfigHyperparameter,
  IConfigHyperparameterValue,
  IConfigHyperparameterSetItem,
} from 'shared/models/Versioning/Blob/ConfigBlob';

export const convertCongifHyperparameter = (
  configHyperparameter: any
): IConfigHyperparameter => {
  return {
    name: configHyperparameter.name,
    value: convertConfigHyperparameterValue(configHyperparameter.value),
  };
};

export const convertConfigHyperparameterValue = (
  value: any
): IConfigHyperparameterValue => {
  if (value.int_value) {
    return { type: 'int', value: Number(value.int_value) };
  }

  if (value.float_value) {
    return { type: 'float', value: Number(value.float_value) };
  }

  if (value.string_value) {
    return { type: 'string', value: String(value.string_value) };
  }

  throw Error('Wrong hyperparameter value');
};

export const convertConfigHyperparameterSetItem = (
  item: any
): IConfigHyperparameterSetItem => {
  if (item.continuous) {
    return {
      type: 'continuous',
      name: item.name,
      intervalBegin: convertConfigHyperparameterValue(
        item.continuous.interval_begin
      ),
      intervalEnd: convertConfigHyperparameterValue(
        item.continuous.interval_end
      ),
      intervalStep: convertConfigHyperparameterValue(
        item.continuous.interval_step
      ),
    };
  }

  if (item.discrete) {
    return {
      name: item.name,
      type: 'discrete',
      values: item.discrete.map(convertConfigHyperparameterValue),
    };
  }

  throw Error('Wrong hyperparameter set');
};

export const convertServerHyperparameters = (
  serverHyperparameters: any
): IConfigHyperparameter[] => {
  return (serverHyperparameters || []).map(convertCongifHyperparameter);
};

export const convertServerHyperparameterSet = (
  serverHyperparameterSet: any
): IConfigHyperparameterSetItem[] => {
  return (serverHyperparameterSet || []).map(
    convertConfigHyperparameterSetItem
  );
};

export const convertConfigBlobToClient = (serverBlob: any): IConfigBlob => {
  const category: IConfigBlob['category'] = 'config';

  const hyperparameters: IConfigHyperparameter[] = convertServerHyperparameters(
    serverBlob.hyperparameters
  );

  const hyperparameterSet: IConfigHyperparameterSetItem[] = convertServerHyperparameterSet(
    serverBlob.hyperparameter_set
  );

  return {
    category,
    data: {
      hyperparameters,
      hyperparameterSet,
    },
  };
};
