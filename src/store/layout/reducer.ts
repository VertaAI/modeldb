import {Reducer} from 'redux';
import {ILayoutState, LayoutActionTypes} from './types';

const initialState: ILayoutState = {
  theme: 'light',
};

const reducer: Reducer<ILayoutState> = (state = initialState, action) => {
  switch (action.type) {
    case LayoutActionTypes.SET_THEME: {
      return { ...state, theme: action.payload };
    }
    default: {
      return state;
    }
  }
};

export {reducer as layoutReducer};
