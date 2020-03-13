import { ConnectedRouter } from 'connected-react-router';
import { createBrowserHistory } from 'history';
import 'normalize.css';
import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import cleanLocalStorageForNewVersion from 'core/shared/utils/cleanLocalStorageForNewVersion';

import App from './App/App';
import './index.css';
import configureStore from './store/configureStore';

const history = createBrowserHistory();

const store = configureStore(history);

const localStorageVersion = '1.0.10';
cleanLocalStorageForNewVersion(localStorageVersion);

ReactDOM.render(
  <Provider store={store}>
    <ConnectedRouter history={history}>
      <App />
    </ConnectedRouter>
  </Provider>,
  document.getElementById('root')
);
