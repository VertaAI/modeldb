import {
  IFilterPayload,
  IFilterState,
  manageFiltersTypes,
} from 'core/features/filter';
import { ComparisonType, PropertyType } from 'core/features/filter/Model';

import dataReducer, { initial } from '../data';

const mockInitialState: IFilterState['data'] = {
  ...initial,
  contexts: {
    ctx: {
      filters: [],
      ctx: {} as any,
      name: 'ctx',
    },
  },
  currentContextName: 'ctx',
};

const mockFilterPayload: IFilterPayload = {
  ctx: 'ctx',
  filter: {
    id: 'af',
    type: PropertyType.METRIC,
    name: 'metric',
    value: 5000,
    comparisonType: ComparisonType.EQUALS,
  },
};

describe('(store/filter/reducer/data)', () => {
  it(`should add filter to specific context on ${
    manageFiltersTypes.ADD_FILTER
  }`, () => {
    const action = {
      type: manageFiltersTypes.ADD_FILTER,
      payload: mockFilterPayload,
    };

    const res = dataReducer(mockInitialState, action);

    expect(res.contexts[action.payload.ctx].filters).toEqual([
      action.payload.filter,
    ]);
  });

  it(`should remove filter from specific context on ${
    manageFiltersTypes.REMOVE_FILTER
  }`, () => {
    const mockState: IFilterState['data'] = {
      ...mockInitialState,
      contexts: {
        ...mockInitialState.contexts,
        [mockFilterPayload.ctx]: {
          ...mockInitialState.contexts[mockFilterPayload.ctx],
          filters: [mockFilterPayload.filter],
        },
      },
    };
    const action = {
      type: manageFiltersTypes.REMOVE_FILTER,
      payload: mockFilterPayload,
    };

    const res = dataReducer(mockState, action);

    expect(res.contexts[action.payload.ctx].filters).toEqual([]);
  });

  it(`should edit filter from specific context on ${
    manageFiltersTypes.EDIT_FILTER
  }`, () => {
    const mockState: IFilterState['data'] = {
      ...mockInitialState,
      contexts: {
        ...mockInitialState.contexts,
        [mockFilterPayload.ctx]: {
          ...mockInitialState.contexts[mockFilterPayload.ctx],
          filters: [mockFilterPayload.filter],
        },
      },
    };
    const action = {
      type: manageFiltersTypes.EDIT_FILTER,
      payload: {
        ...mockFilterPayload,
        filter: {
          ...mockFilterPayload.filter,
          comparisonType: ComparisonType.MORE,
        },
      } as IFilterPayload,
    };

    const res = dataReducer(mockState, action);

    expect(res.contexts[action.payload.ctx].filters).toEqual([
      action.payload.filter,
    ]);
  });
});

export default undefined;
