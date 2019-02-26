import HyperparamsColDef from './Hyperparams';
import MetricsColDef from './Metrics';
import ModelRecordColDef from './ModelRecord';
import styles from './ColumnDefs.module.css';

export const columnDefinitions = [
  {
    headerName: 'Model Record',
    field: 'data',
    width: 500,
    cellRendererFramework: ModelRecordColDef,
    cellClass: [styles.cell, styles.modelDescription]
  },
  {
    headerName: 'Metrics',
    field: 'metrics',
    cellRendererFramework: MetricsColDef,
    width: 260,
    cellClass: styles.cell
  },
  {
    headerName: 'Hyperparameters',
    width: 300,
    field: 'hyperparameters',
    cellRendererFramework: HyperparamsColDef,
    cellClass: styles.cell
  }
];

export const defaultColDefinitions = {
  autoHeight: true
};
