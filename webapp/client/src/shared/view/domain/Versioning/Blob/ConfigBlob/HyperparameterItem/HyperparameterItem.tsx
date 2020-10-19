import * as React from 'react';

import { IConfigHyperparameter } from 'shared/models/Versioning/Blob/ConfigBlob';
import Hyperparameter from '../Hyperparameter/Hyperparameter';

import HyperparameterValue from '../HyperparameterValue/HyperparameterValue';

const HyperparameterItem = ({
  hyperparameter,
  rootStyles,
  valueStyles,
}: {
  hyperparameter: IConfigHyperparameter;
  rootStyles?: React.CSSProperties;
  valueStyles?: React.CSSProperties;
}) => {
  return (
    <Hyperparameter
      name={hyperparameter.name}
      rootStyles={rootStyles}
      valueStyles={valueStyles}
    >
      <HyperparameterValue value={hyperparameter.value} />
    </Hyperparameter>
  );
};

export default HyperparameterItem;
