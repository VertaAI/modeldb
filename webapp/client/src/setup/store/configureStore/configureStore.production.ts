import { ApolloClient } from 'apollo-boost';
import { routerMiddleware } from 'connected-react-router';
import { History } from 'history';
import { AnyAction, applyMiddleware, createStore, Store } from 'redux';
import reduxThunk, { ThunkMiddleware } from 'redux-thunk';

import ServiceFactory from 'services/ServiceFactory';

import {
  createRootReducer,
  IApplicationState,
  IThunkActionDependencies,
} from '../store';

export default function configureStore(
  history: History,
  initialState: IApplicationState,
  extraMiddlewares: any = [],
  apolloClient: ApolloClient<any>
): Store<IApplicationState> {
  const reduxThunkExtraArgument: IThunkActionDependencies = {
    ServiceFactory,
    history,
    apolloClient,
  };

  const store = createStore<IApplicationState, any, any, any>(
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
