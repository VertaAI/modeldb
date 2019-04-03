import { action } from 'typesafe-actions';

import { ActionResult } from 'store/store';

import {
  IColumnMetaData,
  IUpdateDashboardConfigAction,
  updateDashboardConfigActionTypes,
} from './types';

export const updateDashboardConfig = (
  columnConfig: Map<string, IColumnMetaData>
): ActionResult<void, IUpdateDashboardConfigAction> => async dispatch => {
  dispatch(
    action(updateDashboardConfigActionTypes.UPDATE_DASHBOARD, columnConfig)
  );
};
