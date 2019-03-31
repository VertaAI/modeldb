import { routerMiddleware } from 'connected-react-router';
import { History } from 'history';
import { applyMiddleware, createStore, Store } from 'redux';
import reduxThunk, { ThunkMiddleware } from 'redux-thunk';

import { createRootReducer, IApplicationState } from './store';

export default function configureStore(
  history: History,
  initialState: IApplicationState
): Store<IApplicationState> {
  const store = createStore(
    createRootReducer(history),
    initialState,
    applyMiddleware(routerMiddleware(history), reduxThunk as ThunkMiddleware<
      IApplicationState
    >)
  );

  return store;
}
