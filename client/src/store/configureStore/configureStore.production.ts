import { routerMiddleware } from 'connected-react-router';
import { History } from 'history';
import { AnyAction, applyMiddleware, createStore, Store } from 'redux';
import reduxThunk, { ThunkMiddleware } from 'redux-thunk';

import { FilterContextPool } from 'models/FilterContextPool';
import ServiceFactory from 'services/ServiceFactory';

import {
  createRootReducer,
  IApplicationState,
  IThunkActionDependencies,
} from '../store';

export default function configureStore(
  history: History,
  initialState: IApplicationState
): Store<IApplicationState> {
  const reduxThunkExtraArgument: IThunkActionDependencies = {
    FilterContextPool,
    ServiceFactory,
  };

  const store = createStore(
    createRootReducer(history),
    initialState,
    applyMiddleware(routerMiddleware(history), reduxThunk.withExtraArgument(
      reduxThunkExtraArgument
    ) as ThunkMiddleware<
      IApplicationState,
      AnyAction,
      IThunkActionDependencies
    >)
  );

  return store;
}
