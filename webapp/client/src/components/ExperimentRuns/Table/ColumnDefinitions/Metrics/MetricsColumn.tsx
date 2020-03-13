import {
  DataTypeProvider,
  DataTypeProviderProps,
} from '@devexpress/dx-react-grid';
import * as React from 'react';

import withProps from 'core/shared/utils/react/withProps';

import SimpleKeyValuesColumn from '../shared/SimpleKeyValuesColumn/SimpleKeyValuesColumn';

const MetricsColumn = withProps(SimpleKeyValuesColumn)({ type: 'metrics' });

const MetricsTypeProvider = (props: DataTypeProviderProps) => (
  <DataTypeProvider formatterComponent={MetricsColumn as any} {...props} />
);

export default MetricsTypeProvider;
