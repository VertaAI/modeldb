import * as React from 'react';

import { IConfigHyperparameterSetItem } from 'shared/models/Versioning/Blob/ConfigBlob';

import Hyperparameter from '../Hyperparameter/Hyperparameter';
import HyperparameterValue from '../HyperparameterValue/HyperparameterValue';

interface ILocalProps {
  hyperparameterSetItem: IConfigHyperparameterSetItem;
  rootStyles?: React.CSSProperties;
  valueStyles?: React.CSSProperties;
}

const HyperparameterSetItem = ({
  hyperparameterSetItem,
  rootStyles,
  valueStyles,
}: ILocalProps) => {
  if (hyperparameterSetItem.type === 'discrete') {
    return (
      <Hyperparameter
        name={hyperparameterSetItem.name}
        rootStyles={rootStyles}
        valueStyles={valueStyles}
      >
        [
        {hyperparameterSetItem.values.map((hp, i) => (
          <>
            <HyperparameterValue value={hp} />
            {i !== hyperparameterSetItem.values.length && ','}
          </>
        ))}
        ]
      </Hyperparameter>
    );
  }

  return (
    <Hyperparameter
      name={hyperparameterSetItem.name}
      rootStyles={rootStyles}
      valueStyles={valueStyles}
    >
      <span>
        (Start=
        <HyperparameterValue value={hyperparameterSetItem.intervalBegin} />,
        End=
        <HyperparameterValue value={hyperparameterSetItem.intervalEnd} />, Step=
        <HyperparameterValue value={hyperparameterSetItem.intervalStep} />)
      </span>
    </Hyperparameter>
  );
};

export default HyperparameterSetItem;
