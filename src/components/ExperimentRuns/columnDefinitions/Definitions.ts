import ArtifactsColDef from './Artifacts';
import styles from './ColumnDefs.module.css';
import HyperparamsColDef from './Hyperparams';
import MetricsColDef from './Metrics';
import ModelRecordColDef from './ModelRecord';
import SummaryColDef from './Summary';

export const defaultColDefinitions = {
  autoHeight: true
};

export const returnColumnDefs = (updatedConfig: any) => {
  return [
    {
      headerName: 'IDs',
      field: 'data',
      width: 200,
      cellRendererFramework: ModelRecordColDef,
      cellClass: [styles.cell, styles.modelDescription],
      hide: !updatedConfig.get('id').checked
    },
    {
      headerName: 'Summary',
      field: 'data',
      cellRendererFramework: SummaryColDef,
      width: 200,
      cellClass: styles.cell,
      hide: !updatedConfig.get('summary').checked
    },
    {
      headerName: 'Metrics',
      field: 'metrics',
      cellRendererFramework: MetricsColDef,
      width: 200,
      cellClass: styles.cell,
      hide: !updatedConfig.get('metrics').checked
    },
    {
      headerName: 'Hyperparameters',
      width: 200,
      field: 'hyperparameters',
      cellRendererFramework: HyperparamsColDef,
      cellClass: styles.cell,
      hide: !updatedConfig.get('hyperparameters').checked
    },
    {
      headerName: 'Artifacts',
      field: 'artifacts',
      cellRendererFramework: ArtifactsColDef,
      width: 260,
      cellClass: styles.cell,
      hide: !updatedConfig.get('artifacts').checked
    },
    {
      headerName: 'Datasets',
      field: 'hyperparameters',
      cellRendererFramework: HyperparamsColDef,
      width: 200,
      cellClass: styles.cell,
      hide: !updatedConfig.get('datasets').checked
    },
    {
      headerName: 'Observations',
      field: 'hyperparameters',
      cellRendererFramework: HyperparamsColDef,
      width: 200,
      cellClass: styles.cell,
      hide: !updatedConfig.get('observations').checked
    }
  ];
};
