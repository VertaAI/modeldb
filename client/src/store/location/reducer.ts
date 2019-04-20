import { LOCATION_CHANGE } from 'connected-react-router';

import { Reducer } from 'redux';
import { ILocationState } from './types';

const initialState: ILocationState = {
  projectId: undefined,
  runId: undefined,
};

export const locationReducer: Reducer<ILocationState> = (
  state = initialState,
  action
) => {
  if (action.type == LOCATION_CHANGE) {
    const pathname = action.payload.location.pathname;
    const parts = pathname.split('/');
    return {
      projectId: parts[2] == '' ? undefined : parts[2],
      runId: parts[4] == '' ? undefined : parts[4],
    };
  }
  return state;
};
