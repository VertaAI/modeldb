import { Reducer } from 'redux';

import {
  FeatureAction,
  hideDeveloperKeyInfoActionType,
  IProjectsPageState,
} from '../types';

const initial: IProjectsPageState['data'] = {
  isShowDeveloperKeyInfo: true,
};

const dataReducer: Reducer<IProjectsPageState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case hideDeveloperKeyInfoActionType: {
      return { ...state, isShowDeveloperKeyInfo: false };
    }
    default:
      return state;
  }
};

export default dataReducer;
