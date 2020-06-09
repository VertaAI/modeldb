import { createAction } from 'typesafe-actions';

import { ActionResult } from 'store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';
import { ILoadEntitiesByTypeResult } from 'services/highLevelSearch/HighLevelSearchService';
import { AppError } from 'core/shared/models/Error';
import normalizeError from 'core/shared/utils/normalizeError';

import {
  ISearchSettings,
  ActiveFilter,
  changePaginationPage,
  changeFilter,
  changeNameOrTag,
  IResultsSorting,
  changeSorting,
} from '../../../shared/models/HighLevelSearch';
import * as Suggestions from 'core/shared/models/HighLevelSearch/Suggestions';
import {
  updateSearchSettingsQueryParams,
  parseSearchSettingsFromPathname,
} from '../url';
import { paginationSettings } from '../constants';
import { updateSuggestionsToLocalStorage } from '../suggestionsFromLocalStorage';

export const loadEntitiesByTypeActions = {
  success: createAction('@@highLevelSearch/LOAD_ENTITIES_BY_TYPE_SUCCESS')<
    ILoadEntitiesByTypeResult
  >(),
  failure: createAction('@@highLevelSearch/LOAD_ENTITIES_BY_TYPE_FAILURE')<
    AppError
  >(),
};
export const loadEntitiesRequest = createAction(
  '@@highLevelSearch/LOAD_ENTITIES_REQUEST'
)<
  ISearchSettings & {
    pageSize: number;
    loadType: 'activeEntitiesAndUpdateOthers' | 'onlyActive';
  }
>();
export const loadEntities = (
  payload: ISearchSettings & {
    pageSize: number;
    loadType: 'activeEntitiesAndUpdateOthers' | 'onlyActive';
  }
): ActionResult<any, any> => async (
  dispatch,
  getState,
  { ServiceFactory, apolloClient }
) => {
  dispatch(loadEntitiesRequest(payload));

  const settings = {
    pagination: {
      currentPage: payload.currentPage,
      pageSize: payload.pageSize,
    },
    searchSettings: payload,
    workspaceName: selectCurrentWorkspaceName(getState()),
  };

  const isEnableRepositories = true;

  if (payload.loadType === 'activeEntitiesAndUpdateOthers') {
    await ServiceFactory.getHighLevelSearchService(
      apolloClient
    ).loadFullEntitiesByTypeAndUpdateOthersCounts(
      {
        onSuccess: data => {
          dispatch(loadEntitiesByTypeActions.success(data));
        },
        onError: error => {
          dispatch(loadEntitiesByTypeActions.failure(normalizeError(error)));
        },
      },
      settings,
      isEnableRepositories
    );
  } else {
    const data = await ServiceFactory.getHighLevelSearchService(
      apolloClient
    ).loadFullEntitiesByType(settings);
    dispatch(loadEntitiesByTypeActions.success(data));
  }
};

export const setSearchValue = (value: string): ActionResult<any, any> => async (
  dispatch,
  getState,
  { history }
) => {
  if (!value) {
    return;
  }
  const searchSettings = parseSearchSettingsFromPathname(history.location);
  const newSearchSettings = changeNameOrTag(value, searchSettings);
  history.push({
    search: updateSearchSettingsQueryParams(
      history.location,
      changeNameOrTag(value, searchSettings)
    ),
  });
  dispatch(
    loadEntities({
      loadType: 'activeEntitiesAndUpdateOthers',
      ...newSearchSettings,
      pageSize: paginationSettings.pageSize,
    })
  );

  addNewRecentSearch(value);
};

export const addNewRecentSearch = (value: string) => {
  updateSuggestionsToLocalStorage(suggestions =>
    Suggestions.addSearch(value, suggestions)
  );
};
export const addFavoriteToSuggestions = (
  recentSuggestion: Suggestions.RecentSearch
) => {
  updateSuggestionsToLocalStorage(suggestions =>
    Suggestions.addFavorite(recentSuggestion, suggestions)
  );
  return {
    type: '@@highLevelSearch/ADD_FAVORITE_TO_SUGGESTIONS',
    payload: recentSuggestion,
  };
};
export const removeFavoriteFromSuggestions = (
  favorite: Suggestions.Favorite
) => {
  updateSuggestionsToLocalStorage(suggestions =>
    Suggestions.removeFavorite(favorite, suggestions)
  );
  return {
    type: '@@highLevelSearch/REMOVE_FAVORITE_FROM_SUGGESTIONS',
    payload: favorite,
  };
};

export const setFilter = (
  payload: ActiveFilter
): ActionResult<any, any> => async (dispatch, getState, { history }) => {
  const searchSettings = parseSearchSettingsFromPathname(history.location);
  const newSearchSettings = changeFilter(payload, searchSettings);
  history.push({
    search: updateSearchSettingsQueryParams(
      history.location,
      newSearchSettings
    ),
  });
  dispatch(
    loadEntities({
      loadType: 'onlyActive',
      ...newSearchSettings,
      pageSize: paginationSettings.pageSize,
    })
  );
};

export const changeSearchSettingsSorting = (
  sorting: IResultsSorting | undefined
): ActionResult<any, any> => async (dispatch, getState, { history }) => {
  const searchSettings = parseSearchSettingsFromPathname(history.location);
  const newSearchSettings = changeSorting(sorting, searchSettings);
  history.push({
    search: updateSearchSettingsQueryParams(
      history.location,
      newSearchSettings
    ),
  });
  dispatch(
    loadEntities({
      loadType: 'onlyActive',
      ...newSearchSettings,
      pageSize: paginationSettings.pageSize,
    })
  );
};

export const changePagination = (
  page: number
): ActionResult<any, any> => async (dispatch, getState, { history }) => {
  const searchSettings = parseSearchSettingsFromPathname(history.location);
  const newSearchSettings = changePaginationPage(page, searchSettings);
  history.push({
    search: updateSearchSettingsQueryParams(
      history.location,
      newSearchSettings
    ),
  });
  dispatch(
    loadEntities({
      loadType: 'onlyActive',
      ...newSearchSettings,
      pageSize: paginationSettings.pageSize,
    })
  );
};
