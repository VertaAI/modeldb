export interface IDashboardConfigState {
  [columnConfig: string]: IColumnConfig;
}

export interface IColumnMetaData {
  checked: boolean;
  name: string;
  label: string;
}

export type IColumnConfig = Map<string, IColumnMetaData>;

export enum updateDashboardConfigActionTypes {
  UPDATE_DASHBOARD = '@@configDashboard/UPDATE_DASHBOARD',
}

export interface IUpdateDashboardConfigAction {
  type: updateDashboardConfigActionTypes.UPDATE_DASHBOARD;
  payload: IColumnConfig;
}
