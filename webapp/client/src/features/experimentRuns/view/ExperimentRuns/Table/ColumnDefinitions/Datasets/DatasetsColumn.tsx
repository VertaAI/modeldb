import * as React from 'react';

import Datasets from 'core/shared/view/domain/ModelRecord/ModelRecordProps/Datasets/Datasets';

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

export default DatasetsColumn;
