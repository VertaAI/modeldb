import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import './index.css';
import {createStore} from 'redux';
import { testReducer } from './reducers';

const store = createStore(testReducer);
ReactDOM.render(<App />, document.getElementById('root'));
