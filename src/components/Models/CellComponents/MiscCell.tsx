import { Cell, CellProps } from 'fixed-data-table-2';
import { Model } from 'models/Model';
import * as React from 'react';

interface IModelsProps {
  models: Model[];
}
type ModelsCellProps = CellProps & IModelsProps;

export class MiscCell extends React.PureComponent<ModelsCellProps> {
  public render() {
    const { models, rowIndex, columnKey, ...props } = this.props;
    const definedRowIndex = rowIndex || 0;
    const updatedDate = models[definedRowIndex].DateUpdated ? models[definedRowIndex].DateUpdated : '';

    return <Cell {...props}>{updatedDate ? `Date updated: ${updatedDate.toDateString()}` : ''}</Cell>;
  }
}
