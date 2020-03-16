import * as React from 'react';

import { IConfigHyperparameterSetItem } from 'core/shared/models/Repository/Blob/ConfigBlob';

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
      <Hyperparameter name={hyperparameterSetItem.name} rootStyles={rootStyles}>
        [
        {hyperparameterSetItem.values.map((hp, i) => (
          <>
            <HyperparameterValue value={hp} rootStyles={valueStyles} />
            {i !== hyperparameterSetItem.values.length && ','}
          </>
        ))}
        ]
      </Hyperparameter>
    );
  }

  return (
    <Hyperparameter name={hyperparameterSetItem.name} rootStyles={rootStyles}>
      <span style={valueStyles}>
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
