import { Reducer } from 'redux';

import { defaultColumnConfig } from './constants';
import {
  IExperimentRunsTableConfigState,
  IFeatureAction,
  toggleColumnVisibilityActionTypes,
} from './types';

const experimentRunsTableConfigInitialState: IExperimentRunsTableConfigState = {
  columnConfig: defaultColumnConfig,
};

export const experimentRunsTableConfigReducer: Reducer<
  IExperimentRunsTableConfigState,
  IFeatureAction
> = (state = experimentRunsTableConfigInitialState, action) => {
  switch (action.type) {
    case toggleColumnVisibilityActionTypes.TOGGLE_SHOWN_COLUMN_ACTION_TYPES: {
      return {
        ...state,
        columnConfig: {
          ...state.columnConfig,
          [action.payload.columnName]: {
            ...state.columnConfig[action.payload.columnName],
            isShown: !state.columnConfig[action.payload.columnName].isShown,
          },
        },
      };
    }
    default: {
      return state;
    }
  }
};
