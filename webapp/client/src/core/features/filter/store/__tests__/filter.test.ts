import {
  makeDefaultTagFilter,
  IFilterData,
  IStringFilterData,
} from 'core/features/filter/Model';
import flushAllPromises from 'core/shared/utils/tests/integrations/flushAllPromises';
import makeSetupIntegrationTest from 'core/shared/utils/tests/integrations/makeSetupIntegrationTest';

import {
  addFilterToCurrentContext,
  setContext,
  removeFilterFromCurrentContext,
  editFilterInCurrentContext,
} from '../actions';
import filtersReducer from '../reducer';
import { selectCurrentContextFilters } from '../selectors';
import {
  IFilterState,
  IFilterRootState,
  IThunkActionDependencies,
} from '../types';

const currentContextName = 'projects';

const makeMockFilterState = (
  defaultFilters: IFilterData[] = []
): IFilterState => {
  return {
    data: {
      contexts: {
        projects: {
          name: currentContextName,
          ctx: {
            name: currentContextName,
            quickFilters: [],
            onApplyFilters: jest.fn(),
          },
          filters: defaultFilters,
        },
      },
      currentContextName,
    },
  };
};

const localStorageFilters = {
  save: (
    localStorage: Storage,
    contextName: string,
    filters: IFilterData[]
  ) => {
    localStorage[`${contextName}_filter`] = JSON.stringify(
      JSON.stringify(filters)
    );
  },
  get: (localStorage: Storage, contextName: string): IFilterData[] | null => {
    const rawFilters = localStorage[`${contextName}_filter`];
    return rawFilters ? JSON.parse(rawFilters) : null;
  },
};

const URLFilters = {
  makePathWithFilters: (path: string, filters: IFilterData[]) => {
    const formattedFilters = encodeURIComponent(JSON.stringify(filters));
    return `${path}?filters=${formattedFilters}`;
  },
  get: (history: History) => {
    const rawFiltersFromURL = new URLSearchParams(history.location.search).get(
      'filters'
    );
    return rawFiltersFromURL
      ? JSON.parse(decodeURIComponent(rawFiltersFromURL))
      : null;
  },
};

import { routerMiddleware } from 'connected-react-router';
import { History } from 'history';
import { createStore, combineReducers, applyMiddleware } from 'redux';
import reduxThunk, { ThunkMiddleware } from 'redux-thunk';

const setupIntegrationTest = makeSetupIntegrationTest<IFilterRootState>({
  configureStore: (
    history: History,
    initialState: IFilterRootState,
    extraMiddlewares: any[] = []
  ) => {
    const deps: IThunkActionDependencies = { history };

    return createStore<IFilterRootState, any, any, any>(
      combineReducers<IFilterRootState>({ filters: filtersReducer }),
      initialState,
      applyMiddleware(
        ...extraMiddlewares,
        routerMiddleware(history),
        reduxThunk.withExtraArgument(deps) as ThunkMiddleware<
          IFilterRootState,
          any,
          IThunkActionDependencies
        >
      )
    );
  },
});

describe('store', () => {
  describe('filter', () => {
    describe('saving filters in localStorage', () => {
      it('should add filters if it not exist', async () => {
        const newFilter = makeDefaultTagFilter('demo');
        const { store } = setupIntegrationTest({
          initialState: { filters: makeMockFilterState() },
        });

        await store.dispatch(addFilterToCurrentContext(newFilter) as any);
        await flushAllPromises();

        expect(
          localStorageFilters.get(localStorage, currentContextName)
        ).toEqual([newFilter]);
      });

      it('should remove filters if a user delete all filters', async () => {
        const { store } = setupIntegrationTest({
          initialState: { filters: makeMockFilterState() },
        });

        const newFilter = makeDefaultTagFilter('demo');
        await store.dispatch(addFilterToCurrentContext(newFilter) as any);
        await store.dispatch(removeFilterFromCurrentContext(newFilter) as any);
        await flushAllPromises();

        expect(
          localStorageFilters.get(localStorage, currentContextName)
        ).toEqual(null);
      });

      it('should change updated filter', async () => {
        const { store } = setupIntegrationTest({
          initialState: { filters: makeMockFilterState() },
        });

        const newFilter = makeDefaultTagFilter('demo');
        const editedFilter: IStringFilterData = {
          ...newFilter,
          value: 'edited filter value',
          invert: true,
        };
        await store.dispatch(addFilterToCurrentContext(newFilter) as any);
        await store.dispatch(editFilterInCurrentContext(editedFilter) as any);
        await flushAllPromises();

        expect(
          localStorageFilters.get(localStorage, currentContextName)
        ).toEqual([editedFilter]);
      });
    });

    describe('saving filters in URL', () => {
      it('should add filters param if it not exist', async () => {
        const newFilter = makeDefaultTagFilter('demo');
        const { history, store } = setupIntegrationTest({
          initialState: { filters: makeMockFilterState() },
        });

        await store.dispatch(addFilterToCurrentContext(newFilter) as any);
        await flushAllPromises();

        expect(URLFilters.get(history)).toEqual([newFilter]);
      });

      it('should remove filters param if a user delete all filters', async () => {
        const { history, store } = setupIntegrationTest({
          initialState: { filters: makeMockFilterState() },
        });

        const newFilter = makeDefaultTagFilter('demo');
        await store.dispatch(addFilterToCurrentContext(newFilter) as any);
        await store.dispatch(removeFilterFromCurrentContext(newFilter) as any);
        await flushAllPromises();

        expect(URLFilters.get(history)).toEqual(null);
      });

      it('should change updated filter in filters param', async () => {
        const { history, store } = setupIntegrationTest({
          initialState: { filters: makeMockFilterState() },
        });

        const newFilter = makeDefaultTagFilter('demo');
        const editedFilter: IStringFilterData = {
          ...newFilter,
          value: 'edited filter value',
          invert: true,
        };
        await store.dispatch(addFilterToCurrentContext(newFilter) as any);
        await store.dispatch(editFilterInCurrentContext(editedFilter) as any);
        await flushAllPromises();

        expect(URLFilters.get(history)).toEqual([editedFilter]);
      });
    });
  });

  describe('restore filters from URL and localStorage', () => {
    const pathname = '/projects';

    it('should apply filters from localStorage if they exist', async () => {
      localStorage.clear();
      const filters = [makeDefaultTagFilter('demo')];
      localStorageFilters.save(localStorage, currentContextName, filters);
      const { store } = setupIntegrationTest({
        initialState: { filters: makeMockFilterState() },
      });

      await store.dispatch(setContext(
        makeMockFilterState().data.contexts[currentContextName].ctx
      ) as any);
      await flushAllPromises();
    });

    it('should apply filters from URL if they filters param exist', async () => {
      const filters = [makeDefaultTagFilter('demo')];
      const { store } = setupIntegrationTest({
        pathname: URLFilters.makePathWithFilters(pathname, filters),
      });

      await store.dispatch(setContext(
        makeMockFilterState().data.contexts[currentContextName].ctx
      ) as any);
      await flushAllPromises();

      expect(selectCurrentContextFilters(store.getState())).toEqual(filters);
    });

    it('should apply filters from URL if filters are in localStorage and in URL', async () => {
      localStorageFilters.save(localStorage, currentContextName, [
        makeDefaultTagFilter('test'),
        makeDefaultTagFilter('test2'),
      ]);
      const filtersInURL = [makeDefaultTagFilter('demo')];
      const { store } = setupIntegrationTest({
        pathname: URLFilters.makePathWithFilters(pathname, filtersInURL),
      });

      await store.dispatch(setContext(
        makeMockFilterState().data.contexts[currentContextName].ctx
      ) as any);
      await flushAllPromises();

      expect(selectCurrentContextFilters(store.getState())).toEqual(
        filtersInURL
      );
    });
  });
});
