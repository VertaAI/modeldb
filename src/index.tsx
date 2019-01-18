import { createBrowserHistory } from 'history';
import 'normalize.css';
import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import App from './App';
import './index.css';
import User from './models/User';
import configureStore from './store/configureStore';
import { IApplicationState } from './store/store';

const history = createBrowserHistory();
function getUser(): User | undefined {
  const storageUser = localStorage.getItem('user');
  if (storageUser) {
    return JSON.parse(storageUser);
  }
  return undefined;
}

const initialState: IApplicationState = {
  layout: {
    user: getUser()
  },
  project: { loading: false },
  projects: {
    loading: false
  }
};

const store = configureStore(history, initialState);

ReactDOM.render(
  <Provider store={store}>
    <App />
  </Provider>,
  document.getElementById('root')
);
