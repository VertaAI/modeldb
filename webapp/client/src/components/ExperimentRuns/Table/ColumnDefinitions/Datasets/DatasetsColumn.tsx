import {
  DataTypeProvider,
  DataTypeProviderProps,
} from '@devexpress/dx-react-grid';
import * as React from 'react';

import Datasets from 'components/ModelRecordProps/Datasets/Datasets';

import { IRow } from '../types';

interface ILocalProps {
  row: IRow;
}

type AllProps = ILocalProps;

class DatasetsColumn extends React.PureComponent<AllProps> {
  public render() {
    const {
      experimentRun: { datasets, id },
      columnContentHeight,
    } = this.props.row;
    return (
      <Datasets
        modelId={id}
        datasets={datasets}
        maxHeight={columnContentHeight}
      />
    );
  }
}

const DatasetsColumnTypeProvider = (props: DataTypeProviderProps) => (
  <DataTypeProvider formatterComponent={DatasetsColumn as any} {...props} />
);

export default DatasetsColumnTypeProvider;
