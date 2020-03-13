import { History } from 'history';
import { action } from 'typesafe-actions';

import { selectCurrentContextFilters } from 'core/features/filter';
import { IFilterData } from 'core/features/filter/Model';
import { AppError } from 'core/shared/models/Error';
import normalizeError from 'core/shared/utils/normalizeError';
import * as Experiment from 'models/Experiment';
import routes from 'routes';
import { handleDeleteEntities } from 'store/shared/deletion';
import { ActionResult } from 'store/store';
import { makeThunkApiRequest } from 'utils/redux/actions';

import { selectExperimentsPagination } from './selectors';
import {
  ILoadExperimentsActions,
  loadExperimentsActionTypes,
  IDeleteExperimentActions,
  deleteExperimentActionTypes,
  IUpdateExperimentDescriptionAction,
  updateExperimentDescriptionActionType,
  updateExperimentTagsActionType,
  IUpdateExperimentTagsAction,
  IChangeExperimentsPagination,
  changeExperimentsPaginationActionType,
  IExperimentsOptions,
  getDefaultExperimentsOptionsActionType,
  ISelectExperimentForDeleting,
  selectExperimentForDeletingActionType,
  IUnselectExperimentForDeleting,
  unselectExperimentForDeletingActionType,
  IDeleteExperimentsActions,
  deleteExperimentsActionTypes,
  IResetExperimentsForDeleting,
  resetExperimentsForDeletingActionType,
} from './types';

export const createExperiment = makeThunkApiRequest(
  '@@datasets/CREATE_EXPERIMENT_REQUEST',
  '@@datasets/CREATE_EXPERIMENT_SUCCESS',
  '@@datasets/CREATE_EXPERIMENT_FAILURE',
  '@@datasets/CREATE_EXPERIMENT_RESET'
)<
  { projectId: string; settings: Experiment.IExperimentCreationSettings },
  { experiment: Experiment.default },
  AppError
>(
  async ({ payload, dependencies: { ServiceFactory } }) => {
    const experiment = await ServiceFactory.getExperimentsService().createExperiment(
      payload.projectId,
      payload.settings
    );
    return { experiment };
  },
  {
    onSuccess: async ({ dependencies, requestPayload, successPayload }) => {
      dependencies.history.push(
        routes.experiments.getRedirectPathWithCurrentWorkspace({
          projectId: requestPayload.projectId,
        })
      );
    },
  }
);

export const loadExperiments = (
  projectId: string,
  filters: IFilterData[]
): ActionResult<void, ILoadExperimentsActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  const pagination = selectExperimentsPagination(getState());

  dispatch(action(loadExperimentsActionTypes.REQUEST, { projectId }));
  await ServiceFactory.getExperimentsService()
    .loadExperiments(projectId, filters, pagination)
    .then(({ experiments, totalCount }) => {
      dispatch(
        action(loadExperimentsActionTypes.SUCCESS, { experiments, totalCount })
      );
    })
    .catch(error => {
      dispatch(
        action(loadExperimentsActionTypes.FAILURE, normalizeError(error))
      );
    });
};

export const deleteExperiment = (
  projectId: string,
  id: string
): ActionResult<void, IDeleteExperimentActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(deleteExperimentActionTypes.REQUEST, { id }));
  await ServiceFactory.getExperimentsService()
    .deleteExperiment(id)
    .then(() => {
      const prevPagination = selectExperimentsPagination(getState());
      dispatch(action(deleteExperimentActionTypes.SUCCESS, { id }));
      handleDeleteEntities({
        prevPagination,
        changePagination: (currentPage: number) =>
          changeExperimentsPagination(projectId, currentPage),
        currentPagination: selectExperimentsPagination(getState()),
        dispatch,
        loadEntities: () =>
          loadExperiments(projectId, selectCurrentContextFilters(getState())),
      });
    })
    .catch(error => {
      dispatch(
        action(deleteExperimentActionTypes.FAILURE, {
          id,
          error: normalizeError(error),
        })
      );
    });
};

export const updateExperimentDescription = (
  id: string,
  description: string
): IUpdateExperimentDescriptionAction => ({
  type: updateExperimentDescriptionActionType.UPDATE_EXPERIMENT_DESCRIPTION,
  payload: { id, description },
});

export const updateExperimentTags = (
  id: string,
  tags: string[]
): IUpdateExperimentTagsAction => ({
  type: updateExperimentTagsActionType.UPDATE_EXPERIMENT_TAGS,
  payload: { id, tags },
});

export const changeExperimentsPaginationWithLoading = (
  projectId: string,
  currentPage: number,
  filters: IFilterData[]
): ActionResult<void, IChangeExperimentsPagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(
    action(changeExperimentsPaginationActionType.CHANGE_CURRENT_PAGE, {
      currentPage,
    })
  );
  saveExperimentsOption(deps.history, projectId, {
    type: 'pagination',
    data: { currentPage: selectExperimentsPagination(getState()).currentPage },
  });
  loadExperiments(projectId, filters)(dispatch, getState, deps);
};
const changeExperimentsPagination = (
  projectId: string,
  currentPage: number
): ActionResult<void, IChangeExperimentsPagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(
    action(changeExperimentsPaginationActionType.CHANGE_CURRENT_PAGE, {
      currentPage,
    })
  );
  saveExperimentsOption(deps.history, projectId, {
    type: 'pagination',
    data: { currentPage: selectExperimentsPagination(getState()).currentPage },
  });
};
export const resetExperimentsPagination = (
  projectId: string
): ActionResult<void, IChangeExperimentsPagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(changeExperimentsPagination(projectId, 0));
};

export const getDefaultExperimentsOptions = (
  projectId: string
): ActionResult<void, any> => async (dispatch, getState, deps) => {
  const optionsFromUrl = getExperimentsOptionsFromUrl();
  const options: IExperimentsOptions = {
    paginationCurrentPage: optionsFromUrl.paginationCurrentPage,
  };

  dispatch(
    action(
      getDefaultExperimentsOptionsActionType.GET_DEFAULT_EXPERIMENTS_OPTIONS,
      {
        options,
      }
    )
  );
};

const saveExperimentsOption = (
  history: History,
  projectId: string,
  option: IOption
) => {
  saveExperimentsOptionInUrl(history, option);
};

const saveExperimentsOptionInUrl = (history: History, option: IOption) => {
  const urlSearchParams = new URLSearchParams(window.location.search);
  if (option.data.currentPage === 0) {
    urlSearchParams.delete('page');
  } else {
    urlSearchParams.set('page', String(option.data.currentPage + 1));
  }
  history.push({
    search: String(urlSearchParams),
  });
};
const getExperimentsOptionsFromUrl = (): IExperimentsOptions => {
  const urlSearchParams = new URLSearchParams(window.location.search);
  const paginationCurrentPage = (() => {
    const pageFromUrl = urlSearchParams.get('page');
    return pageFromUrl ? Number(pageFromUrl) - 1 : undefined;
  })();
  return {
    paginationCurrentPage,
  };
};

interface IOption {
  type: 'pagination';
  data: { currentPage: number };
}

export const selectExperimentForDeleting = (
  id: string
): ISelectExperimentForDeleting => ({
  type:
    selectExperimentForDeletingActionType.SELECT_EXPERIMENT_RUN_FOR_DELETING,
  payload: { id },
});

export const unselectExperimentForDeleting = (
  id: string
): IUnselectExperimentForDeleting => ({
  type:
    unselectExperimentForDeletingActionType.UNSELECT_EXPERIMENT_RUN_FOR_DELETING,
  payload: { id },
});

export const resetExperimentsForDeleting = (): IResetExperimentsForDeleting => ({
  type: resetExperimentsForDeletingActionType.RESET_EXPERIMENTS_FOR_DELETING,
});

export const deleteExperiments = (
  projectId: string,
  ids: string[]
): ActionResult<void, IDeleteExperimentsActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(deleteExperimentsActionTypes.REQUEST, { ids }));

  await ServiceFactory.getExperimentsService()
    .deleteExperiments(ids)
    .then(() => {
      const prevPagination = selectExperimentsPagination(getState());
      dispatch(action(deleteExperimentsActionTypes.SUCCESS, { ids }));
      handleDeleteEntities({
        prevPagination,
        changePagination: (currentPage: number) =>
          changeExperimentsPagination(projectId, currentPage),
        currentPagination: selectExperimentsPagination(getState()),
        dispatch,
        loadEntities: () =>
          loadExperiments(projectId, selectCurrentContextFilters(getState())),
      });
    })
    .catch(error => {
      dispatch(
        action(deleteExperimentsActionTypes.FAILURE, normalizeError(error))
      );
    });
};
