import { History } from 'history';
import { action } from 'typesafe-actions';

import { selectCurrentContextFilters } from 'core/features/filter';
import { IFilterData } from 'core/features/filter/Model';
import { AppError } from 'core/shared/models/Error';
import normalizeError from 'core/shared/utils/normalizeError';
import * as Dataset from 'models/Dataset';
import { IWorkspace } from 'models/Workspace';
import routes from 'routes';
import { handleDeleteEntities } from 'store/shared/deletion';
import { ActionResult } from 'store/store';
import { selectCurrentWorkspaceNameOrDefault } from 'store/workspaces';
import { makeThunkApiRequest } from 'utils/redux/actions';

import { selectDatasetsPagination } from './selectors';
import {
  ILoadDatasetsActions,
  loadDatasetsActionTypes,
  IDeleteDatasetActions,
  deleteDatasetActionTypes,
  ILoadDatasetActions,
  loadDatasetActionTypes,
  IUpdateDataset,
  updateDatasetActionType,
  IUpdateDatasetDesc,
  updateDatasetDescActionType,
  IUpdateDatasetTags,
  updateDatasetTagsActionType,
  IChangeDatasetsPagination,
  changeDatasetsPaginationActionType,
  IDatasetsOptions,
  getDefaultDatasetsOptionsActionType,
  ISelectDatasetForDeleting,
  selectDatasetForDeletingActionType,
  IUnselectDatasetForDeleting,
  unselectDatasetForDeletingActionType,
  IDeleteDatasetsActions,
  deleteDatasetsActionTypes,
  IResetDatasetsForDeleting,
  resetDatasetsForDeletingActionType,
} from './types';

export const createDataset = makeThunkApiRequest(
  '@@datasets/CREATE_DATASET_REQUEST',
  '@@datasets/CREATE_DATASET_SUCCESS',
  '@@datasets/CREATE_DATASET_FAILURE',
  '@@datasets/CREATE_DATASET_RESET'
)<
  { settings: Dataset.IDatasetCreationSettings },
  { dataset: Dataset.Dataset },
  AppError
>(
  async ({ payload, dependencies: { ServiceFactory } }) => {
    const dataset = await ServiceFactory.getDatasetsService().createDataset(
      payload.settings
    );
    return { dataset };
  },
  {
    onSuccess: async ({ dependencies, getState, successPayload }) => {
      dependencies.history.push(
        routes.datasetSummary.getRedirectPath({
          datasetId: successPayload.dataset.id,
          workspaceName: selectCurrentWorkspaceNameOrDefault(getState()),
        })
      );
    },
  }
);

export const loadDatasets = (
  filters: IFilterData[],
  workspaceName: IWorkspace['name']
): ActionResult<void, ILoadDatasetsActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(loadDatasetsActionTypes.REQUEST));

  const pagination = selectDatasetsPagination(getState());

  await ServiceFactory.getDatasetsService()
    .loadDatasets(filters, pagination, workspaceName)
    .then(datasets => {
      dispatch(action(loadDatasetsActionTypes.SUCCESS, { datasets }));
    })
    .catch(error => {
      dispatch(action(loadDatasetsActionTypes.FAILURE, normalizeError(error)));
    });
};

export const deleteDataset = (
  id: string,
  workspaceName: IWorkspace['name']
): ActionResult<void, IDeleteDatasetActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(deleteDatasetActionTypes.REQUEST, { id }));

  await ServiceFactory.getDatasetsService()
    .deleteDataset(id)
    .then(() => {
      const prevPagination = selectDatasetsPagination(getState());
      dispatch(action(deleteDatasetActionTypes.SUCCESS, { id }));
      handleDeleteEntities({
        changePagination: changeDatasetsPagination,
        prevPagination,
        currentPagination: selectDatasetsPagination(getState()),
        dispatch,
        loadEntities: () =>
          loadDatasets(selectCurrentContextFilters(getState()), workspaceName),
      });
    })
    .catch(error => {
      dispatch(
        action(deleteDatasetActionTypes.FAILURE, {
          id,
          error: normalizeError(error),
        })
      );
    });
};

export const loadDataset = (
  id: string,
  workspaceName: IWorkspace['name']
): ActionResult<void, ILoadDatasetActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(loadDatasetActionTypes.REQUEST, { id }));

  await ServiceFactory.getDatasetsService()
    .loadDataset(id, workspaceName)
    .then(dataset => {
      dispatch(action(loadDatasetActionTypes.SUCCESS, { dataset }));
    })
    .catch(error => {
      dispatch(
        action(loadDatasetActionTypes.FAILURE, {
          error: normalizeError(error),
          id,
        })
      );
    });
};

export const updateDatasetDesc = (
  id: string,
  description: string
): IUpdateDatasetDesc => ({
  type: updateDatasetDescActionType.UPDATE_DATASET_DESC,
  payload: { id, description },
});

export const updateDatasetTags = (
  modelId: string,
  tags: string[]
): IUpdateDatasetTags => ({
  type: updateDatasetTagsActionType.UPDATE_DATASET_TAGS,
  payload: { datasetId: modelId, tags },
});

export const updateDataset = (dataset: Dataset.Dataset): IUpdateDataset => {
  return {
    type: updateDatasetActionType.UPDATE_DATASET,
    payload: { dataset },
  };
};

export const changeDatasetsPaginationWithLoading = (
  currentPage: number,
  filters: IFilterData[],
  workspaceName: IWorkspace['name']
): ActionResult<void, IChangeDatasetsPagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(
    action(changeDatasetsPaginationActionType.CHANGE_CURRENT_PAGE, {
      currentPage,
    })
  );
  saveDatasetsOption(deps.history, {
    type: 'pagination',
    data: { currentPage: selectDatasetsPagination(getState()).currentPage },
  });
  loadDatasets(filters, workspaceName)(dispatch, getState, deps);
};
const changeDatasetsPagination = (
  currentPage: number
): ActionResult<void, IChangeDatasetsPagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(
    action(changeDatasetsPaginationActionType.CHANGE_CURRENT_PAGE, {
      currentPage,
    })
  );
  saveDatasetsOption(deps.history, {
    type: 'pagination',
    data: { currentPage: selectDatasetsPagination(getState()).currentPage },
  });
};
export const resetDatasetsPagination = (): ActionResult<
  void,
  IChangeDatasetsPagination
> => async (dispatch, getState, deps) => {
  dispatch(changeDatasetsPagination(0));
};

export const getDefaultDatasetsOptions = (): ActionResult<void, any> => async (
  dispatch,
  getState,
  deps
) => {
  const optionsFromUrl = getDatasetsOptionsFromUrl();
  const options: IDatasetsOptions = {
    paginationCurrentPage: optionsFromUrl.paginationCurrentPage,
  };

  dispatch(
    action(getDefaultDatasetsOptionsActionType.GET_DEFAULT_DATASETS_OPTIONS, {
      options,
    })
  );
};

const saveDatasetsOption = (history: History, option: IOption) => {
  saveDatasetsOptionInUrl(history, option);
};

const saveDatasetsOptionInUrl = (history: History, option: IOption) => {
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
const getDatasetsOptionsFromUrl = (): IDatasetsOptions => {
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

export const selectDatasetForDeleting = (
  id: string
): ISelectDatasetForDeleting => ({
  type: selectDatasetForDeletingActionType.SELECT_DATASET_FOR_DELETING,
  payload: { id },
});

export const unselectDatasetForDeleting = (
  id: string
): IUnselectDatasetForDeleting => ({
  type: unselectDatasetForDeletingActionType.UNSELECT_DATASET_FOR_DELETING,
  payload: { id },
});

export const resetDatasetsForDeleting = (): IResetDatasetsForDeleting => ({
  type: resetDatasetsForDeletingActionType.RESET_DATASETS_FOR_DELETING,
});

export const deleteDatasets = (
  ids: string[],
  workspaceName: IWorkspace['name']
): ActionResult<void, IDeleteDatasetsActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(deleteDatasetsActionTypes.REQUEST, { ids }));

  await ServiceFactory.getDatasetsService()
    .deleteDatasets(ids)
    .then(() => {
      const prevPagination = selectDatasetsPagination(getState());
      dispatch(action(deleteDatasetsActionTypes.SUCCESS, { ids }));
      handleDeleteEntities({
        changePagination: changeDatasetsPagination,
        prevPagination,
        currentPagination: selectDatasetsPagination(getState()),
        dispatch,
        loadEntities: () =>
          loadDatasets(selectCurrentContextFilters(getState()), workspaceName),
      });
    })
    .catch(error => {
      dispatch(
        action(deleteDatasetsActionTypes.FAILURE, normalizeError(error))
      );
    });
};
