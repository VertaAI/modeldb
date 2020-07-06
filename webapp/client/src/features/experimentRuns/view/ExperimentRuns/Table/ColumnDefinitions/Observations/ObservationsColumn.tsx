import * as React from 'react';

import Observations from 'shared/view/domain/ModelRecord/ModelRecordProps/Observations/Observations/Observations';

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

export default ObservationsColumn;
