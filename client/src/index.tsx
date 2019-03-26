import { ConnectedRouter } from 'connected-react-router';
import { createBrowserHistory } from 'history';
import jwtDecode from 'jwt-decode';
import 'normalize.css';
import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import App from './App';

import configureStore from './store/configureStore';

import './index.css';

export const history = createBrowserHistory();

const store = configureStore(history);

ReactDOM.render(
  <Provider store={store}>
    <ConnectedRouter history={history}>
      <App />
    </ConnectedRouter>
  </Provider>,
  document.getElementById('root')
);
