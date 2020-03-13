export interface IExperimentRunsTableConfigRootState {
  experimentRunsTableConfig: IExperimentRunsTableConfigState;
}

export interface IExperimentRunsTableConfigState {
  columnConfig: IColumnConfig;
}

export interface IColumnConfig {
  summary: IColumnMetaData;
  metrics: IColumnMetaData;
  hyperparameters: IColumnMetaData;
  artifacts: IColumnMetaData;
  observations: IColumnMetaData;
  attributes: IColumnMetaData;
  datasets: IColumnMetaData;
  codeVersion: IColumnMetaData;
}

export interface IColumnMetaData {
  isShown: boolean;
  name: keyof IColumnConfig;
  label: string;
  order: number;
}

export enum toggleColumnVisibilityActionTypes {
  TOGGLE_SHOWN_COLUMN_ACTION_TYPES = '@@experimentRunsTableConfig/TOGGLE_COLUMN_VISIBILITY',
}
export interface IToggleColumnVisibilityAction {
  type: toggleColumnVisibilityActionTypes.TOGGLE_SHOWN_COLUMN_ACTION_TYPES;
  payload: { columnName: keyof IColumnConfig };
}

export type IFeatureAction = IToggleColumnVisibilityAction;
