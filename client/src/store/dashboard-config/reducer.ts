import { Reducer } from 'redux';
import { IDashboardConfigState, IUpdateDashboardConfigAction, updateDashboardConfigActionTypes } from './types';

const dashboardInitialState: IDashboardConfigState = {
  columnConfig: new Map([
    ['id', { checked: true, name: 'id', label: 'Ids' }],
    ['summary', { checked: true, name: 'summary', label: 'Summary' }],
    ['metrics', { checked: true, name: 'metrics', label: 'Metrics' }],
    ['hyperparameters', { checked: true, name: 'hyperparameters', label: 'Hyperparameters' }],
    ['artifacts', { checked: true, name: 'artifacts', label: 'Artifacts' }],
    ['datasets', { checked: false, name: 'datasets', label: 'Dataset' }]
    // ['observations', { checked: false, name: 'observations', label: 'Observations' }]
  ]),
};

export const dashboardConfigReducer: Reducer<IDashboardConfigState, IUpdateDashboardConfigAction> = (
  state = dashboardInitialState,
  action: IUpdateDashboardConfigAction
) => {
  switch (action.type) {
    case updateDashboardConfigActionTypes.UPDATE_DASHBOARD: {
      return { ...state, columnConfig: action.payload };
    }
    default: {
      return state;
    }
  }
};
