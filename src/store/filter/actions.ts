import { ActionResult } from 'store/store';
import { action } from 'typesafe-actions';
import { IFilterData } from '../../components/FilterSelect/FilterSelect';
import { MetaData } from '../../models/IMetaData';
import ServiceFactory from '../../services/ServiceFactory';
import { fetchProjectsAction, fetchProjectsActionTypes, projectFetchModelsAction, projectFetchModelsActionTypes } from '../project';
import { applyModelsFilterAction, applyProjectsFilterAction, filtersActionTypes, initContextAction, searchFiltersAction } from './types';

export function searchFilters<T extends MetaData>(searchString: string): ActionResult<void, searchFiltersAction> {
  return async (dispatch, getState) => {
    dispatch(action(filtersActionTypes.SEARCH_FILTERS_REQUEST));
    const svc = ServiceFactory.getSearchAndFiltersService<T>(getState().filters.context);
    if (svc != null) {
      await svc.searchFilters(searchString).then(res => {
        dispatch(action(filtersActionTypes.SEARCH_FILTERS_RESULT, res));
      });
    }
  };
}
export function initContext<T extends MetaData>(
  ctx: string,
  meta: MetaData,
  isFiltersSupport: boolean = false
): ActionResult<void, initContextAction> {
  return async (dispatch, getState) => {
    dispatch(action(filtersActionTypes.CHANGE_CONTEXT, ctx));
    dispatch(action(filtersActionTypes.IS_FILTERS_SUPPORT, isFiltersSupport));
    const svc = ServiceFactory.getSearchAndFiltersService<T>(ctx);
    if (svc != null) {
      svc.setMetaData(meta);
    }
  };
}

export function resetContext(): ActionResult<void, initContextAction> {
  return async (dispatch, getState) => {
    dispatch(action(filtersActionTypes.CHANGE_CONTEXT, ''));
    dispatch(action(filtersActionTypes.IS_FILTERS_SUPPORT, false));
  };
}

export const applyProjectsFilter = (data?: IFilterData[]): ActionResult<void, applyProjectsFilterAction | fetchProjectsAction> => async (
  dispatch,
  getState
) => {
  dispatch(action(filtersActionTypes.APPLY_PROJECTS_FILTER_REQUEST));
  dispatch(action(fetchProjectsActionTypes.FETCH_PROJECTS_REQUEST));

  await ServiceFactory.getDataService()
    .getProjects(data)
    .then(res => {
      dispatch(action(filtersActionTypes.APPLY_PROJECTS_FILTER_SUCCESS, data));
      dispatch(action(fetchProjectsActionTypes.FETCH_PROJECTS_SUCESS, res));
    });
};

export const applyModelsFilter = (data?: IFilterData[]): ActionResult<void, applyModelsFilterAction | projectFetchModelsAction> => async (
  dispatch,
  getState
) => {
  const project = getState().project;
  if (project && project.data) {
    const projId: string = project.data.Id;

    dispatch(action(filtersActionTypes.APPLY_MODELS_FILTER_REQUEST));
    dispatch(action(projectFetchModelsActionTypes.FETCH_MODELS_REQUEST));
    await ServiceFactory.getDataService()
      .getProject(projId)
      .then(res => {
        dispatch(action(filtersActionTypes.APPLY_MODELS_FILTER_SUCCESS, data));
        dispatch(action(projectFetchModelsActionTypes.FETCH_MODELS_SUCESS, res));
      });
  }
};
