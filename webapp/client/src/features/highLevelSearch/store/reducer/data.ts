import { ActionType, createReducer } from 'typesafe-actions';
import { combineReducers } from 'redux';
import { RouterAction } from 'connected-react-router';

import { IEntitiesResults } from 'shared/models/HighLevelSearch';
import routes from 'shared/routes';
import {
  initialCommunication,
  requestingCommunication,
  successfullCommunication,
} from 'shared/utils/redux/communication';
import { mapObj } from 'shared/utils/collection';

import * as actions from '../actions';
import { IHighLevelSearchState } from '../types';

const initial: IHighLevelSearchState['data'] = {
  redirectTo: null,

  entitiesResults: {
    datasets: {
      communication: initialCommunication,
      data: { totalCount: 0, data: undefined },
    },
    experimentRuns: {
      communication: initialCommunication,
      data: { totalCount: 0, data: undefined },
    },
    experiments: {
      communication: initialCommunication,
      data: { totalCount: 0, data: undefined },
    },
    projects: {
      communication: initialCommunication,
      data: { totalCount: 0, data: undefined },
    },
    repositories: {
      communication: initialCommunication,
      data: { totalCount: 0, data: undefined },
    },
  },
};

const entitiesResultsReducer = createReducer<
  IHighLevelSearchState['data']['entitiesResults'],
  ActionType<typeof actions>
>(initial.entitiesResults)
  .handleAction(actions.loadEntitiesRequest, (state, action) => {
    if (action.payload.loadType === 'activeEntitiesAndUpdateOthers') {
      return mapObj(
        res => ({ ...res, communication: requestingCommunication }),
        state
      ) as IEntitiesResults;
    } else {
      return {
        ...state,
        [action.payload.type]: {
          communication: requestingCommunication,
          data: {},
        },
      };
    }
  })
  .handleAction(actions.loadEntitiesByTypeActions.success, (state, action) => {
    return {
      ...state,
      [action.payload.type]: {
        communication: successfullCommunication,
        data: action.payload.data,
      },
    };
  });

const redirectToReducer = (
  state: IHighLevelSearchState['data']['redirectTo'] = initial.redirectTo,
  action: RouterAction
): IHighLevelSearchState['data']['redirectTo'] => {
  switch (action.type) {
    case '@@router/LOCATION_CHANGE': {
      if (!routes.highLevelSearch.getMatch(action.payload.location.pathname)) {
        return action.payload.location;
      }
      return state;
    }
    default:
      return state;
  }
};

export default combineReducers<IHighLevelSearchState['data']>({
  redirectTo: redirectToReducer,
  entitiesResults: entitiesResultsReducer,
});
