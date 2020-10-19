import { History } from 'history';
import { action } from 'typesafe-actions';

import { IFilterData } from 'shared/models/Filters';
import normalizeError from 'shared/utils/normalizeError';
import { ActionResult } from 'setup/store/store';

import { selectCurrentContextFilters } from 'features/filter';
import { handleDeleteEntities } from 'features/shared/deletion';
import { selectDatasetVersionsPagination } from './selectors';
import {
  ILoadDatasetVersionsActions,
  IDeleteDatasetVersionActions,
  loadDatasetVersionsActionTypes,
  deleteDatasetVersionActionTypes,
  ILoadDatasetVersionActions,
  loadDatasetVersionActionTypes,
  IChangeDatasetVersionsPagination,
  changeDatasetVersionsPaginationActionType,
  IDatasetVersionsOptions,
  getDefaultDatasetVersionsOptionsActionType,
  IUpdateDatasetVersionDesc,
  updateDatasetVersionDescActionType,
  IUpdateDatasetVersionTags,
  updateDatasetVersionTagsActionType,
  ILoadComparedDatasetVersionsActions,
  loadComparedDatasetVersionsActionTypes,
  ISelectDatasetVersionForDeleting,
  selectDatasetVersionForDeletingActionType,
  IUnselectDatasetVersionForDeleting,
  unselectDatasetVersionForDeletingActionType,
  IDeleteDatasetVersionsActions,
  deleteDatasetVersionsActionTypes,
  IResetDatasetVersionsForDeleting,
  resetDatasetVersionsForDeletingActionType,
  ISelectAllDatasetVersionsForDeleting,
  selectAllDatasetVersionsForDeletingActionType,
  ILoadDatasetVersionExperimentRunsActions,
  loadDatasetVersionExperimentRunsActionTypes,
} from './types';

export const loadDatasetVersions = (
  datasetId: string,
  filters: IFilterData[]
): ActionResult<void, ILoadDatasetVersionsActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(loadDatasetVersionsActionTypes.REQUEST, { datasetId }));

  const pagination = selectDatasetVersionsPagination(getState());

  await ServiceFactory.getDatasetVersionsService()
    .loadDatasetVersions(datasetId, filters, pagination)
    .then((res) => {
      dispatch(
        action(loadDatasetVersionsActionTypes.SUCCESS, { datasetVersions: res })
      );
    })
    .catch((error) => {
      dispatch(
        action(loadDatasetVersionsActionTypes.FAILURE, normalizeError(error))
      );
    });
};

export const deleteDatasetVersion = (
  datasetId: string,
  id: string
): ActionResult<void, IDeleteDatasetVersionActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(deleteDatasetVersionActionTypes.REQUEST, { id }));

  await ServiceFactory.getDatasetVersionsService()
    .deleteDatasetVersion(id)
    .then(() => {
      const prevPagination = selectDatasetVersionsPagination(getState());
      dispatch(action(deleteDatasetVersionActionTypes.SUCCESS, { id }));
      handleDeleteEntities({
        prevPagination,
        currentPagination: selectDatasetVersionsPagination(getState()),
        changePagination: changeDatasetVersionsPagination,
        dispatch,
        loadEntities: () =>
          loadDatasetVersions(
            datasetId,
            selectCurrentContextFilters(getState())
          ),
      });
    })
    .catch((error) => {
      dispatch(
        action(deleteDatasetVersionActionTypes.FAILURE, {
          id,
          error: normalizeError(error),
        })
      );
    });
};

export const loadDatasetVersion = (
  workspaceName: any,
  id: string,
  datasetId: string
): ActionResult<void, ILoadDatasetVersionActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(loadDatasetVersionActionTypes.REQUEST, { id }));

  await ServiceFactory.getDatasetVersionsService()
    .loadDatasetVersion(workspaceName, id, datasetId)
    .then((datasetVersion) => {
      dispatch(
        action(loadDatasetVersionActionTypes.SUCCESS, { datasetVersion })
      );
    })
    .catch((error) => {
      dispatch(
        action(loadDatasetVersionActionTypes.FAILURE, normalizeError(error))
      );
    });
};

export const loadDatasetVersionExperimentRuns = (
  workspaceName: any,
  datasetVersionId: string
): ActionResult<void, ILoadDatasetVersionExperimentRunsActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(
    action(loadDatasetVersionExperimentRunsActionTypes.REQUEST, {
      datasetVersionId,
    })
  );

  await ServiceFactory.getExperimentRunsService()
    .loadExperimentRunsByDatasetVersionId(workspaceName, datasetVersionId)
    .then((res) => {
      dispatch(
        action(loadDatasetVersionExperimentRunsActionTypes.SUCCESS, {
          datasetVersionId,
          experimentRuns: res.data,
        })
      );
    })
    .catch((error) => {
      dispatch(
        action(loadDatasetVersionExperimentRunsActionTypes.FAILURE, {
          datasetVersionId,
          error: normalizeError(error),
        })
      );
    });
};

export const updateDatasetVersionDesc = (
  id: string,
  description: string
): IUpdateDatasetVersionDesc => ({
  type: updateDatasetVersionDescActionType.UPDATE_DATASET_VERSION_DESC,
  payload: { id, description },
});

export const updateDatasetVersionTags = (
  id: string,
  tags: string[]
): IUpdateDatasetVersionTags => ({
  type: updateDatasetVersionTagsActionType.UPDATE_DATASET_VERSION_TAGS,
  payload: { id, tags },
});

export const changeDatasetVersionsPaginationWithLoading = (
  datasetId: string,
  currentPage: number,
  filters: IFilterData[]
): ActionResult<void, IChangeDatasetVersionsPagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(
    action(changeDatasetVersionsPaginationActionType.CHANGE_CURRENT_PAGE, {
      currentPage,
    })
  );
  saveDatasetVersionsOption(deps.history, {
    type: 'pagination',
    data: {
      currentPage: selectDatasetVersionsPagination(getState()).currentPage,
    },
  });
  loadDatasetVersions(datasetId, filters)(dispatch, getState, deps);
};
const changeDatasetVersionsPagination = (
  currentPage: number
): ActionResult<void, IChangeDatasetVersionsPagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(
    action(changeDatasetVersionsPaginationActionType.CHANGE_CURRENT_PAGE, {
      currentPage,
    })
  );
  saveDatasetVersionsOption(deps.history, {
    type: 'pagination',
    data: {
      currentPage: selectDatasetVersionsPagination(getState()).currentPage,
    },
  });
};
export const resetDatasetVersionsPagination = (): ActionResult<
  void,
  IChangeDatasetVersionsPagination
> => async (dispatch, getState, deps) => {
  dispatch(changeDatasetVersionsPagination(0));
};

export const getDefaultDatasetVersionsOptions = (): ActionResult<
  void,
  any
> => async (dispatch, getState, deps) => {
  const optionsFromUrl = getDatasetVersionsOptionsFromUrl();
  const options: IDatasetVersionsOptions = {
    paginationCurrentPage: optionsFromUrl.paginationCurrentPage,
  };

  dispatch(
    action(
      getDefaultDatasetVersionsOptionsActionType.GET_DEFAULT_DATASET_VERSIONS_OPTIONS,
      {
        options,
      }
    )
  );
};

const saveDatasetVersionsOption = (history: History, option: IOption) => {
  saveDatasetVersionsOptionInUrl(history, option);
};

const saveDatasetVersionsOptionInUrl = (history: History, option: IOption) => {
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
const getDatasetVersionsOptionsFromUrl = (): IDatasetVersionsOptions => {
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

export const loadComparedDatasetVersions = (
  workspaceName: any,
  datasetId: string,
  datasetVersionId1: string,
  datasetVersionId2: string
): ActionResult<void, ILoadComparedDatasetVersionsActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(
    action(loadComparedDatasetVersionsActionTypes.REQUEST, {
      datasetVersionId1,
      datasetVersionId2,
    })
  );

  Promise.all([
    ServiceFactory.getDatasetVersionsService().loadDatasetVersion(
      datasetVersionId1,
      datasetId
    ),
    ServiceFactory.getDatasetVersionsService().loadDatasetVersion(
      datasetVersionId2,
      datasetId
    ),
  ])
    .then(([datasetVersion1, datasetVersion2]) => {
      dispatch(
        action(loadComparedDatasetVersionsActionTypes.SUCCESS, {
          datasetVersion1,
          datasetVersion2,
        })
      );
    })
    .catch((error) => {
      dispatch(
        action(
          loadComparedDatasetVersionsActionTypes.FAILURE,
          normalizeError(error)
        )
      );
    });
};

export const selectDatasetVersionForDeleting = (
  id: string
): ISelectDatasetVersionForDeleting => ({
  type:
    selectDatasetVersionForDeletingActionType.SELECT_DATASET_VERSION_FOR_DELETING,
  payload: { id },
});
export const selectAllDatasetVersionsForDeleting = (): ISelectAllDatasetVersionsForDeleting => ({
  type:
    selectAllDatasetVersionsForDeletingActionType.SELECT_ALL_DATASET_VERSIONS_FOR_DELETING,
});

export const unselectDatasetVersionForDeleting = (
  id: string
): IUnselectDatasetVersionForDeleting => ({
  type:
    unselectDatasetVersionForDeletingActionType.UNSELECT_DATASET_VERSION_FOR_DELETING,
  payload: { id },
});

export const resetDatasetVersionsForDeleting = (): IResetDatasetVersionsForDeleting => ({
  type:
    resetDatasetVersionsForDeletingActionType.RESET_DATASET_VERSIONS_FOR_DELETING,
});

export const deleteDatasetVersions = (
  datasetId: string,
  ids: string[]
): ActionResult<void, IDeleteDatasetVersionsActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(deleteDatasetVersionsActionTypes.REQUEST, { ids }));

  await ServiceFactory.getDatasetVersionsService()
    .deleteDatasetVersions(ids)
    .then(() => {
      const prevPagination = selectDatasetVersionsPagination(getState());
      dispatch(action(deleteDatasetVersionsActionTypes.SUCCESS, { ids }));
      handleDeleteEntities({
        prevPagination,
        currentPagination: selectDatasetVersionsPagination(getState()),
        changePagination: changeDatasetVersionsPagination,
        dispatch,
        loadEntities: () =>
          loadDatasetVersions(
            datasetId,
            selectCurrentContextFilters(getState())
          ),
      });
    })
    .catch((error) => {
      dispatch(
        action(deleteDatasetVersionsActionTypes.FAILURE, normalizeError(error))
      );
    });
};
