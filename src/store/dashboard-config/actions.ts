import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import { IColumnMetaData, IUpdateDashboardConfigAction, updateDashboardConfigActionTypes } from './types';

export const updateDashboardConfig = (
  columnConfig: Map<string, IColumnMetaData>
): ActionResult<void, IUpdateDashboardConfigAction> => async (dispatch, getState) => {
  dispatch(action(updateDashboardConfigActionTypes.UPDATE_DASHBOARD, columnConfig));
};
