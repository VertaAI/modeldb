import { createBrowserHistory } from 'history';
import jwtDecode from 'jwt-decode';
import 'normalize.css';
import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import App from './App';
import './index.css';
import User from './models/User';
import ServiceFactory from './services/ServiceFactory';
import { InvitationStatus } from './store/collaboration';
import configureStore from './store/configureStore';
import { IFilterContextData } from './store/filter';
import { IApplicationState } from './store/store';

export const history = createBrowserHistory();
function getUser(): User | null {
  try {
    const authenticatedService = ServiceFactory.getAuthenticationService();

    if (authenticatedService.authenticated) {
      return jwtDecode<User>(authenticatedService.idToken);
    }
    return null;
  } catch {
    return null;
  }
}

const initialState: IApplicationState = {
  collaboration: {
    changeAccess: { status: InvitationStatus.None },
    changeOwner: { status: InvitationStatus.None },
    inviteNewCollaborator: { status: InvitationStatus.None },
    removeAccess: { status: InvitationStatus.None }
  },
  experimentRuns: {
    loading: false
  },
  filters: {
    // contexts: new Map<string, IFilterContextData>()
    contexts: {}
  },
  layout: {
    authenticated: false,
    loading: false,
    user: getUser()
  },
  modelRecord: {
    loading: false
  },
  projects: { loading: false }
};

const store = configureStore(history, initialState);

ReactDOM.render(
  <Provider store={store}>
    <App />
  </Provider>,
  document.getElementById('root')
);
