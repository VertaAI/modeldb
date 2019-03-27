export interface IColumnMetaData {
  checked: boolean;
  name: string;
  label: string;
}

export interface IDashboardConfigState {
  [columnConfig: string]: Map<string, IColumnMetaData>;
}

export enum updateDashboardConfigActionTypes {
  UPDATE_DASHBOARD = '@@configDashboard/UPDATE_DASHBOARD',
}

export interface IUpdateDashboardConfigAction {
  type: updateDashboardConfigActionTypes.UPDATE_DASHBOARD;
  payload: Map<string, IColumnMetaData>;
}
