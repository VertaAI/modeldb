import { routerMiddleware } from 'connected-react-router';
import { History } from 'history';
import { applyMiddleware, createStore, Store } from 'redux';
import { composeWithDevTools } from 'redux-devtools-extension';
import createLogger from 'redux-logger';
import reduxThunk, { ThunkMiddleware } from 'redux-thunk';
import { createRootReducer, IApplicationState } from './store';

export default function configureStore(history: History, initialState: IApplicationState): Store<IApplicationState> {
  const composeEnhancers = composeWithDevTools({});

  const store = createStore(
    createRootReducer(history),
    initialState,
    composeEnhancers(applyMiddleware(routerMiddleware(history), createLogger, reduxThunk as ThunkMiddleware<IApplicationState>))
  );

  return store;
}

// TODO create here configureStore for production version
