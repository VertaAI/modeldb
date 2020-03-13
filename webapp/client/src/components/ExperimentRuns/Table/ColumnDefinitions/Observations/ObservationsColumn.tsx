import {
  DataTypeProviderProps,
  DataTypeProvider,
} from '@devexpress/dx-react-grid';
import * as React from 'react';

import Observations from 'components/ModelRecordProps/Observations/Observations/Observations';

import { IRow } from '../types';

interface ILocalProps {
  row: IRow;
}

type AllProps = ILocalProps;

class ObservationsColumn extends React.PureComponent<AllProps> {
  public render() {
    const { experimentRun, columnContentHeight } = this.props.row;
    return (
      <Observations
        observations={experimentRun.observations}
        maxHeight={columnContentHeight}
      />
    );
  }
}

const ObservationsTypeProvider = (props: DataTypeProviderProps) => {
  return (
    <DataTypeProvider
      formatterComponent={ObservationsColumn as any}
      {...props}
    />
  );
};

export default ObservationsTypeProvider;
