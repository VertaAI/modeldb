import { History } from 'history';
import { action, createAction } from 'typesafe-actions';

import { IFilterData } from 'core/features/filter/Model';
import { IPagination } from 'core/shared/models/Pagination';
import { ISorting } from 'core/shared/models/Sorting';
import normalizeError from 'core/shared/utils/normalizeError';
import {
  ISetEntitiesCommentsWithAuthor,
  setEntitiesCommentsWithAuthor,
} from 'features/comments';
import {
  resetCurrentContextFilters,
  selectCurrentContextAppliedFilters,
} from 'core/features/filter';
import { handleDeleteEntities } from 'store/shared/deletion';
import { ActionResult } from 'store/store';

import ModelRecord from 'models/ModelRecord';
import {
  selectExperimentRunsPagination,
  selectExperimentRunsSorting,
} from './selectors';
import {
  ILoadExperimentRunActions,
  ILoadExperimentRunsActions,
  ILoadSequentialChartDataActions,
  ILazyLoadChartDataActions,
  IUpdateExpRunTags,
  updateExpRunTagsActionType,
  IUpdateExpRunDesc,
  updateExpRunDescActionType,
  loadExperimentRunActionTypes,
  loadExperimentRunsActionTypes,
  loadSequentialChartDataActionTypes,
  lazyLoadChartDataActionTypes,
  cleanChartDataPayload,
  IChangePagination,
  changePaginationActionType,
  IChangeSorting,
  changeSortingActionType,
  ISuccessLoadExperimentRunsPayload,
  ISuccessLoadSequentialChartDataPayload,
  ISuccessLazyLoadChartDataPayload,
  ICleanChartDataPayload,
  deleteExperimentRunActionTypes,
  IDeleteExperimentRunActions,
  IExprRunsOptions as IExperimentRunsOptions,
  IGetDefaultExperimentRunsSettings,
  getDefaultExperimentRunsSettingsActionType,
  IDeleteExperimentRunArtifactActions,
  deleteExperimentRunArtifactActionTypes,
  selectExperimentRunForDeletingActionType,
  ISelectExperimentRunForDeleting,
  IUnselectExperimentRunForDeleting,
  unselectExperimentRunForDeletingActionType,
  IDeleteExperimentRunsActions,
  deleteExperimentRunsActionTypes,
  IResetExperimentRunsForDeleting,
  resetExperimentRunsForDeletingActionType,
  IResetExperimentRunsSettings,
  ISelectAllExperimentRunsForDeleting,
  selectAllExperimentRunsForDeletingActionType,
} from './types';

export const loadExperimentRuns = (
  projectId: string,
  filters?: IFilterData[]
): ActionResult<
  void,
  ILoadExperimentRunsActions | ISetEntitiesCommentsWithAuthor
> => async (dispatch, getState, { ServiceFactory }) => {
  dispatch(action(loadExperimentRunsActionTypes.REQUEST));

  const pagination = selectExperimentRunsPagination(getState());
  const sorting = selectExperimentRunsSorting(getState());

  await ServiceFactory.getExperimentRunsService()
    .loadExperimentRuns(projectId, filters, pagination, sorting)
    .then(({ data, totalCount }) => {
      const payload: ISuccessLoadExperimentRunsPayload = {
        totalCount,
        experimentRuns: data.map(({ experimentRun }) => experimentRun),
      };
      dispatch(action(loadExperimentRunsActionTypes.SUCCESS, payload));
      dispatch(
        setEntitiesCommentsWithAuthor(
          data.map(({ comments, experimentRun }) => ({
            comments,
            entityId: experimentRun.id,
          }))
        )
      );
    })
    .catch(error => {
      dispatch(
        action(loadExperimentRunsActionTypes.FAILURE, normalizeError(error))
      );
    });
};

export const cleanChartData = (): ActionResult<
  void,
  ICleanChartDataPayload
> => dispatch => {
  dispatch(action(cleanChartDataPayload.CLEAN_CHART_DATA));
};

export const chartsPageSettings = {
  pageSize: 50,
  datapointLimit: 500,
};

export const loadSequentialChartData = (
  projectId: string,
  pagination: IPagination,
  filters?: IFilterData[]
): ActionResult<void, ILoadSequentialChartDataActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(loadSequentialChartDataActionTypes.REQUEST));

  const paginationUpdated: IPagination = {
    ...pagination,
    currentPage: pagination.currentPage + 1,
  };
  pagination = paginationUpdated;

  const numOfCalls = Math.floor(
    pagination.totalCount / chartsPageSettings.pageSize
  );
  const callLimit =
    chartsPageSettings.datapointLimit / chartsPageSettings.pageSize - 1;
  const maxCalls = numOfCalls > callLimit ? callLimit : numOfCalls;

  await ServiceFactory.getExperimentRunsService()
    .loadExperimentRuns(projectId, filters, pagination, null)
    .then(({ data }) => {
      if (data && data.length > 0) {
        const payload: ISuccessLoadSequentialChartDataPayload = {
          sequentialChartData: data.map(({ experimentRun }) => experimentRun),
        };

        dispatch(action(loadSequentialChartDataActionTypes.SUCCESS, payload));
        if (maxCalls > pagination.currentPage) {
          dispatch(loadSequentialChartData(projectId, pagination, filters));
        }
      }
    })
    .catch(error => {
      dispatch(
        action(
          loadSequentialChartDataActionTypes.FAILURE,
          normalizeError(error)
        )
      );
    });
};

export const lazyLoadChartData = (
  projectId: string,
  filters?: IFilterData[]
): ActionResult<void, ILazyLoadChartDataActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(lazyLoadChartDataActionTypes.REQUEST));

  await ServiceFactory.getExperimentRunsService()
    .lazyLoadChartData(projectId, filters)
    .then(({ lazyChartData, totalCount }) => {
      const payload: ISuccessLazyLoadChartDataPayload = {
        lazyChartData,
        totalCount,
      };

      const pagination: IPagination = {
        currentPage: 0,
        pageSize: chartsPageSettings.pageSize,
        totalCount,
      };
      if (totalCount > chartsPageSettings.pageSize) {
        dispatch(loadSequentialChartData(projectId, pagination, filters));
      }

      dispatch(action(lazyLoadChartDataActionTypes.SUCCESS, payload));
    })
    .catch(error => {
      dispatch(
        action(lazyLoadChartDataActionTypes.FAILURE, normalizeError(error))
      );
    });
};

export const loadExperimentRun = (
  projectId: string,
  modelId: string
): ActionResult<
  void,
  ILoadExperimentRunActions | ISetEntitiesCommentsWithAuthor
> => async (dispatch, getState, { ServiceFactory }) => {
  dispatch(action(loadExperimentRunActionTypes.REQUEST, modelId));

  await ServiceFactory.getExperimentRunsService()
    .loadModelRecord(modelId)
    .then(res => {
      dispatch(action(loadExperimentRunActionTypes.SUCCESS, res.experimentRun));
      dispatch(
        setEntitiesCommentsWithAuthor([
          { entityId: res.experimentRun.id, comments: res.comments },
        ])
      );
    })
    .catch(error => {
      dispatch(
        action(loadExperimentRunActionTypes.FAILURE, {
          id: modelId,
          error: normalizeError(error),
        })
      );
    });
};

export const deleteExperimentRun = (
  projectId: string,
  id: string
): ActionResult<void, IDeleteExperimentRunActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(deleteExperimentRunActionTypes.REQUEST, { id }));

  await ServiceFactory.getExperimentRunsService()
    .deleteExperimentRun(id)
    .then(() => {
      const prevPagination = selectExperimentRunsPagination(getState());
      dispatch(action(deleteExperimentRunActionTypes.SUCCESS, { id }));
      handleDeleteEntities({
        prevPagination,
        currentPagination: selectExperimentRunsPagination(getState()),
        changePagination: currentPage =>
          changePagination(projectId, { currentPage }),
        dispatch,
        loadEntities: () =>
          loadExperimentRuns(
            projectId,
            selectCurrentContextAppliedFilters(getState())
          ),
      });
    })
    .catch(error => {
      dispatch(
        action(deleteExperimentRunActionTypes.FAILURE, {
          id,
          error: normalizeError(error),
        })
      );
    });
};

export const updateExpRunTags = (
  id: string,
  tags: string[]
): IUpdateExpRunTags => ({
  type: updateExpRunTagsActionType.UPDATE_EXPERIMENT_RUN_TAGS,
  payload: { id, tags },
});

export const updateExpRunDesc = (
  id: string,
  description: string
): IUpdateExpRunDesc => ({
  type: updateExpRunDescActionType.UPDATE_EXPERIMENT_RUN_DESC,
  payload: { id, description },
});

export const changePaginationWithLoadingExperimentRuns = (
  projectId: string,
  currentPage: number,
  filters: IFilterData[]
): ActionResult<void, ILoadExperimentRunsActions | IChangePagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(changePagination(projectId, { currentPage }));
  loadExperimentRuns(projectId, filters)(dispatch, getState, deps);
};
const changePagination = (
  projectId: string,
  payload: IChangePagination['payload']
): ActionResult<void, IChangePagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(action(changePaginationActionType.CHANGE_CURRENT_PAGE, payload));
  saveExperimentRunsOption(deps.history, projectId, {
    type: 'pagination',
    data: selectExperimentRunsPagination(getState()),
  });
};
export const resetPagination = (
  projectId: string
): ActionResult<void, IChangePagination> => async (dispatch, getState) => {
  const pagination = selectExperimentRunsPagination(getState());
  dispatch(changePagination(projectId, { ...pagination, currentPage: 0 }));
};

export const changeSortingWithLoadingExperimentRuns = (
  projectId: string,
  sorting: ISorting | null,
  filters: IFilterData[]
): ActionResult<void, IChangeSorting | IChangePagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(changeSorting(projectId, sorting));
  loadExperimentRuns(projectId, filters)(dispatch, getState, deps);
};
export const changeSorting = (
  projectId: string,
  sorting: ISorting | null
): ActionResult<void, IChangeSorting | IChangePagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(
    action(changeSortingActionType.CHANGE_SORTING, {
      sorting,
    })
  );
  saveExperimentRunsOption(deps.history, projectId, {
    type: 'sorting',
    data: selectExperimentRunsSorting(getState()),
  });
  dispatch(resetPagination(projectId));
};

export const getExperimentRunsOptions = (
  projectId: string
): ActionResult<void, IGetDefaultExperimentRunsSettings> => async (
  dispatch,
  getState,
  deps
) => {
  const optionsFromUrl = getExperimentRunsOptionsFromUrl();
  const optionsFromLocalStorage = getExperimentRunsOptionsFromLocalStorage(
    projectId
  );
  const options: IExperimentRunsOptions = {
    pagination: optionsFromUrl.pagination || optionsFromLocalStorage.pagination,
    sorting: optionsFromUrl.sorting || optionsFromLocalStorage.sorting,
  };

  if (options.pagination) {
    saveExperimentRunsOption(deps.history, projectId, {
      type: 'pagination',
      data: options.pagination,
    });
  }
  if (options.sorting) {
    saveExperimentRunsOption(deps.history, projectId, {
      type: 'sorting',
      data: options.sorting,
    });
  }

  dispatch(
    action(
      getDefaultExperimentRunsSettingsActionType.GET_DEFAULT_EXPERIMENT_RUNS_SETTINGS,
      {
        options,
      }
    )
  );
};

const saveExperimentRunsOption = (
  history: History,
  projectId: string,
  option: IOption
) => {
  saveExperimentRunsOptionInUrl(history, option);
  saveExperimentRunsOptionInLocalStorage(projectId, option);
};

const saveExperimentRunsOptionInUrl = (history: History, option: IOption) => {
  const urlSearchParams = new URLSearchParams(window.location.search);
  if (option.type === 'pagination') {
    if (option.data.currentPage === 0) {
      urlSearchParams.delete('page');
    } else {
      urlSearchParams.set('page', String(option.data.currentPage + 1));
    }
  } else {
    if (!option.data) {
      urlSearchParams.delete('sortKey');
      urlSearchParams.delete('sortDirection');
    } else {
      urlSearchParams.set(
        'sortKey',
        `${option.data.columnName}.${option.data.fieldName}`
      );
      urlSearchParams.set('sortDirection', option.data.direction);
    }
  }
  history.push({
    search: String(urlSearchParams),
  });
};
const getExperimentRunsOptionsFromUrl = (): IExperimentRunsOptions => {
  const urlSearchParams = new URLSearchParams(window.location.search);
  const paginationCurrentPage = (() => {
    const pageFromUrl = urlSearchParams.get('page');
    return pageFromUrl ? Number(pageFromUrl) - 1 : undefined;
  })();
  const sorting: ISorting | undefined = (() => {
    const sortKeyFromUrl = urlSearchParams.get('sortKey');
    const sortDirectionFromUrl = urlSearchParams.get('sortDirection');
    if (sortKeyFromUrl && sortDirectionFromUrl) {
      const [columnName, fieldName] = sortKeyFromUrl.split('.');
      return {
        columnName,
        fieldName,
        direction: sortDirectionFromUrl as ISorting['direction'],
      };
    }
  })();
  return {
    sorting,
    pagination: paginationCurrentPage
      ? { currentPage: paginationCurrentPage }
      : undefined,
  };
};

const experimentRunsOptionLocalStorageKey = 'exprRunsOptions';
type IOption =
  | { type: 'pagination'; data: { currentPage: number } }
  | { type: 'sorting'; data: ISorting | null };
const saveExperimentRunsOptionInLocalStorage = (
  projectId: string,
  option: IOption
) => {
  const options: IExperimentRunsOptions = getExperimentRunsOptionsFromLocalStorage(
    projectId
  );
  const updatedOptions: IExperimentRunsOptions = (() => {
    if (option.type === 'pagination') {
      return {
        ...options,
        pagination: option.data.currentPage === 0 ? undefined : option.data,
      };
    }
    return { ...options, sorting: option.data || undefined };
  })();
  const localStorageKey = `${experimentRunsOptionLocalStorageKey}_${projectId}`;
  if (!updatedOptions.pagination && !updatedOptions.sorting) {
    localStorage.removeItem(localStorageKey);
  } else {
    localStorage.setItem(localStorageKey, JSON.stringify(updatedOptions));
  }
};
const getExperimentRunsOptionsFromLocalStorage = (
  projectId: string
): IExperimentRunsOptions => {
  const res =
    localStorage[`${experimentRunsOptionLocalStorageKey}_${projectId}`];
  return res ? JSON.parse(res) : {};
};

export const deleteExperimentRunArtifact = (
  experimentRunId: string,
  artifactKey: string
): ActionResult<void, IDeleteExperimentRunArtifactActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(
    action(deleteExperimentRunArtifactActionTypes.REQUEST, {
      id: experimentRunId,
      artifactKey,
    })
  );

  await ServiceFactory.getExperimentRunsService()
    .deleteArtifact(experimentRunId, artifactKey)
    .then(() => {
      dispatch(
        action(deleteExperimentRunArtifactActionTypes.SUCCESS, {
          id: experimentRunId,
          artifactKey,
        })
      );
    })
    .catch(error => {
      dispatch(
        action(deleteExperimentRunArtifactActionTypes.FAILURE, {
          id: experimentRunId,
          artifactKey,
          error: normalizeError(error),
        })
      );
    });
};

export const selectExperimentRunForDeleting = (
  id: string
): ISelectExperimentRunForDeleting => ({
  type:
    selectExperimentRunForDeletingActionType.SELECT_EXPERIMENT_RUN_FOR_DELETING,
  payload: { id },
});
export const selectAllExperimentRunsForDeleting = (): ISelectAllExperimentRunsForDeleting => ({
  type:
    selectAllExperimentRunsForDeletingActionType.SELECT_ALL_EXPERIMENT_RUNS_FOR_DELETING,
});

export const unselectExperimentRunForDeleting = (
  id: string
): IUnselectExperimentRunForDeleting => ({
  type:
    unselectExperimentRunForDeletingActionType.UNSELECT_EXPERIMENT_RUN_FOR_DELETING,
  payload: { id },
});

export const resetExperimentRunsForDeleting = (): IResetExperimentRunsForDeleting => ({
  type:
    resetExperimentRunsForDeletingActionType.RESET_EXPERIMENT_RUNS_FOR_DELETING,
});

export const deleteExperimentRuns = (
  projectId: string,
  ids: string[]
): ActionResult<void, IDeleteExperimentRunsActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(deleteExperimentRunsActionTypes.REQUEST, { ids }));

  await ServiceFactory.getExperimentRunsService()
    .deleteExperimentRuns(ids)
    .then(() => {
      const prevPagination = selectExperimentRunsPagination(getState());
      dispatch(action(deleteExperimentRunsActionTypes.SUCCESS, { ids }));
      handleDeleteEntities({
        changePagination: currentPage =>
          changePagination(projectId, { currentPage }),
        dispatch,
        prevPagination,
        currentPagination: selectExperimentRunsPagination(getState()),
        loadEntities: () =>
          loadExperimentRuns(
            projectId,
            selectCurrentContextAppliedFilters(getState())
          ),
      });
    })
    .catch(error => {
      dispatch(
        action(deleteExperimentRunsActionTypes.FAILURE, normalizeError(error))
      );
    });
};

export const resetExperimentRunsSettings = (
  projectId: string
): ActionResult<void, IResetExperimentRunsSettings> => async dispatch => {
  return Promise.all([
    dispatch(changePagination(projectId, { currentPage: 0 })),
    dispatch(changeSorting(projectId, null)),
    dispatch(resetCurrentContextFilters(false)),
  ]).then(_ => dispatch(loadExperimentRuns(projectId)));
};

export const setExperimentRuns = createAction(
  '@@experimentRuns/setExperimentRuns'
)<ModelRecord[]>();
