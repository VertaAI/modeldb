import {
  DataTypeProvider,
  DataTypeProviderProps,
} from '@devexpress/dx-react-grid';
import * as React from 'react';

import withProps from 'core/shared/utils/react/withProps';

import SimpleKeyValuesColumn from '../shared/SimpleKeyValuesColumn/SimpleKeyValuesColumn';

const HyperparametersColumn = withProps(SimpleKeyValuesColumn)({
  type: 'hyperparameters',
});

const HyperparametersTypeProvider = (props: DataTypeProviderProps) => (
  <DataTypeProvider
    formatterComponent={HyperparametersColumn as any}
    {...props}
  />
);

export default HyperparametersTypeProvider;
