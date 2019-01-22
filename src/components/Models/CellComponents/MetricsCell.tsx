import { Cell, CellProps } from 'fixed-data-table-2';
import { Model } from 'models/Model';
import { IModelMetric } from 'models/ModelMetric';
import * as React from 'react';

interface IModelsProps {
  models: Model[];
}
type ModelsCellProps = CellProps & IModelsProps;

export class MetricsCell extends React.PureComponent<ModelsCellProps> {
  public render() {
    const { models, rowIndex, columnKey, ...props } = this.props;
    const definedRowIndex = rowIndex || 0;

    return (
      <Cell {...props}>
        {models[definedRowIndex].ModelMetric.map((value: IModelMetric, key: number) => {
          return (
            <div key={key}>
              {value.key}: {value.value}
            </div>
          );
        })}
      </Cell>
    );
  }
}
