import { Cell, CellProps } from 'fixed-data-table-2';
import { Model } from 'models/Model';
import * as React from 'react';
import { Link } from 'react-router-dom';
import styles from './IdCell.module.css';

interface IModelsProps {
  models: Model[];
}
type ModelsCellProps = CellProps & IModelsProps;

export class IdCell extends React.PureComponent<ModelsCellProps> {
  public render() {
    const { models, rowIndex, columnKey, ...props } = this.props;
    const definedRowIndex = rowIndex || 0;
    const model = models[definedRowIndex];
    return (
      <Cell {...props}>
        <Link className={styles.link} to={`/project/${model.ProjectId}/model/${model.Id}`}>
          Model ID: {model.Id}
        </Link>
      </Cell>
    );
  }
}
