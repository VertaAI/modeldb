import { RouterAction, LocationChangeAction } from 'connected-react-router';
import * as History from 'core/shared/utils/History';
import routes from 'core/shared/routes';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';

import { IRepositoryNavigationState, RepositoryHistory } from './types';

const isRepositoryPathname = (pathname: string) => {
  return routes.repositoryData.getMatch(pathname, false);
};

const initialState: IRepositoryNavigationState = { history: null };

const getHistoryMove = (
  action: LocationChangeAction<any>,
  history: RepositoryHistory
) => {
  if (action.payload.isFirstRendering) {
    return null;
  }

  const pathname = action.payload.location.pathname;
  if (pathname === History.getBackItem(history)) {
    return History.back;
  }
  if (pathname === History.getForwardItem(history)) {
    return History.forward;
  }

  return null;
};

export default function reducer(
  state: IRepositoryNavigationState = initialState,
  action: RouterAction
): IRepositoryNavigationState {
  switch (action.type) {
    case '@@router/LOCATION_CHANGE': {
      if (isRepositoryPathname(action.payload.location.pathname)) {
        if (!state.history) {
          return {
            ...state,
            history: History.make(action.payload.location.pathname),
          };
        }
        switch (action.payload.action) {
          case 'PUSH': {
            return {
              ...state,
              history: History.push(
                action.payload.location.pathname,
                state.history
              ),
            };
          }
          case 'POP': {
            const historyMove = getHistoryMove(action, state.history);
            return historyMove
              ? {
                  ...state,
                  history: historyMove(state.history)[0],
                }
              : state;
          }
          case 'REPLACE': {
            return {
              ...state,
              history: History.make(action.payload.location.pathname),
            };
          }
          default:
            return exhaustiveCheck(action.payload.action, '');
        }
      }
      return initialState;
    }
    default:
      return state;
  }
}
