import React from 'react';
import reactDom from 'react-dom';
import { Provider } from 'react-redux';
import {createStore} from 'redux';
import App from './App';
import './index.css';
import reducer from './redux/reducer';

const store = createStore(reducer);

reactDom.render(
  <Provider store={store}>
    <App />
  </Provider>,
  document.getElementById('root'));
