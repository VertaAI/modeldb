import { createBrowserHistory } from 'history';
import 'normalize.css';
import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import App from './App';
import './index.css';
import configureStore from './store/configureStore';
import { IApplicationState } from './store/store';

const history = createBrowserHistory();
const initialState: IApplicationState = {
  layout: {
    theme: 'dark'
  }
};

const store = configureStore(history, initialState);

ReactDOM.render(
  <Provider store={store}>
    <App history={{}} />
  </Provider>,
  document.getElementById('root')
);
