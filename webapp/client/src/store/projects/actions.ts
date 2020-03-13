import { History } from 'history';
import { action, createAction } from 'typesafe-actions';

import { IFilterData } from 'core/features/filter/Model';
import normalizeError from 'core/shared/utils/normalizeError';
import { Markdown } from 'core/shared/utils/types';
import { Project } from 'models/Project';
import { ActionResult } from 'store/store';

import { IWorkspace } from 'models/Workspace';
import { selectCurrentContextFilters } from 'core/features/filter';
import { handleDeleteEntities } from 'store/shared/deletion';

import { selectProjectsPagination } from './selectors';
import {
  ILoadProjectsActions,
  IDeleteProjectActions,
  IUpdateProjectAction,
  loadProjectsActionTypes,
  deleteProjectActionTypes,
  updateProjectActionType,
  IUpdateReadmeActions,
  updateReadmeActionTypes,
  IUpdateProjectTags,
  updateProjectTagsActionType,
  IUpdateProjectDesc,
  updateProjectDescActionType,
  ILoadProjectActions,
  loadProjectActionTypes,
  IProjectsOptions,
  IChangeProjectsPagination,
  changeProjectsPaginationActionType,
  getDefaultProjectsOptionsActionType,
  ISelectProjectForDeleting,
  selectProjectForDeletingActionType,
  IUnselectProjectForDeleting,
  unselectProjectForDeletingActionType,
  deleteProjectsActionTypes,
  IDeleteProjectsActions,
  ILoadProjectDatasetsActions,
  loadProjectDatasetsActionTypes,
  IResetProjectsForDeleting,
  resetProjectsForDeletingActionType,
} from './types';

export const loadProjects = (
  filters: IFilterData[],
  workspaceName: IWorkspace['name']
): ActionResult<void, ILoadProjectsActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(loadProjectsActionTypes.REQUEST));

  const pagination = selectProjectsPagination(getState());

  await ServiceFactory.getProjectsService()
    .loadProjects(filters, pagination, workspaceName)
    .then(res => {
      dispatch(action(loadProjectsActionTypes.SUCCESS, { projects: res }));
    })
    .catch(error => {
      dispatch(action(loadProjectsActionTypes.FAILURE, normalizeError(error)));
    });
};

export const loadProject = (
  projectId: string
): ActionResult<void, ILoadProjectActions> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
  dispatch(action(loadProjectActionTypes.REQUEST, { projectId }));

  await ServiceFactory.getProjectsService()
    .loadProject(projectId)
    .then(res => {
      dispatch(action(loadProjectActionTypes.SUCCESS, { project: res }));
    })
    .catch(error => {
      dispatch(
        action(loadProjectActionTypes.FAILURE, {
          error: normalizeError(error),
          projectId,
        })
      );
    });
};

export const updateProject = (project: Project): IUpdateProjectAction => ({
  type: updateProjectActionType.UPDATE_PROJECT,
  payload: project,
});

export const deleteProject = (
  id: string,
  workspaceName: IWorkspace['name']
): ActionResult<void, IDeleteProjectActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(deleteProjectActionTypes.REQUEST, id));

  await ServiceFactory.getProjectsService()
    .deleteProject(id)
    .then(res => {
      const prevPagination = selectProjectsPagination(getState());
      dispatch(action(deleteProjectActionTypes.SUCCESS, id));
      handleDeleteEntities({
        prevPagination,
        changePagination: changeProjectsPagination,
        currentPagination: selectProjectsPagination(getState()),
        dispatch,
        loadEntities: () =>
          loadProjects(selectCurrentContextFilters(getState()), workspaceName),
      });
    })
    .catch(error => {
      dispatch(
        action(deleteProjectActionTypes.FAILURE, {
          projectId: id,
          error: normalizeError(error),
        })
      );
    });
};

export const updateProjectReadme = (
  id: string,
  readme: Markdown
): ActionResult<void, IUpdateReadmeActions> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
  dispatch(action(updateReadmeActionTypes.REQUEST, { projectId: id, readme }));

  await ServiceFactory.getProjectsService()
    .updateReadme(id, readme)
    .then(_ => {
      dispatch(
        action(updateReadmeActionTypes.SUCCESS, { projectId: id, readme })
      );
    })
    .catch(error => {
      dispatch(action(updateReadmeActionTypes.FAILURE, normalizeError(error)));
    });
};

export const updateProjectTags = (
  projectId: string,
  tags: string[]
): IUpdateProjectTags => ({
  type: updateProjectTagsActionType.UPDATE_PROJECT_TAGS,
  payload: { projectId, tags },
});

export const updateProjectDesc = (
  projectId: string,
  description: string
): IUpdateProjectDesc => ({
  type: updateProjectDescActionType.UPDATE_PROJECT_DESC,
  payload: { projectId, description },
});

export const changeProjectsPaginationWithLoading = (
  currentPage: number,
  filters: IFilterData[],
  workspaceName: IWorkspace['name']
): ActionResult<void, IChangeProjectsPagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(
    action(changeProjectsPaginationActionType.CHANGE_CURRENT_PAGE, {
      currentPage,
    })
  );
  saveProjectsOption(deps.history, {
    type: 'pagination',
    data: { currentPage: selectProjectsPagination(getState()).currentPage },
  });
  loadProjects(filters, workspaceName)(dispatch, getState, deps);
};
const changeProjectsPagination = (
  currentPage: number
): ActionResult<void, IChangeProjectsPagination> => async (
  dispatch,
  getState,
  deps
) => {
  dispatch(
    action(changeProjectsPaginationActionType.CHANGE_CURRENT_PAGE, {
      currentPage,
    })
  );
  saveProjectsOption(deps.history, {
    type: 'pagination',
    data: { currentPage: selectProjectsPagination(getState()).currentPage },
  });
};
export const resetProjectsPagination = (): ActionResult<
  void,
  IChangeProjectsPagination
> => async (dispatch, getState, deps) => {
  dispatch(changeProjectsPagination(0));
};

export const getDefaultProjectsOptions = (): ActionResult<void, any> => async (
  dispatch,
  getState,
  deps
) => {
  const optionsFromUrl = getProjectsOptionsFromUrl();
  const options: IProjectsOptions = {
    paginationCurrentPage: optionsFromUrl.paginationCurrentPage,
  };

  dispatch(
    action(getDefaultProjectsOptionsActionType.GET_DEFAULT_PROJECTS_OPTIONS, {
      options,
    })
  );
};

const saveProjectsOption = (history: History, option: IOption) => {
  saveProjectsOptionInUrl(history, option);
};

const URLPaginationCurrentPageParam = 'page';
const saveProjectsOptionInUrl = (history: History, option: IOption) => {
  const urlSearchParams = new URLSearchParams(window.location.search);
  if (option.data.currentPage === 0) {
    urlSearchParams.delete(URLPaginationCurrentPageParam);
  } else {
    urlSearchParams.set(
      URLPaginationCurrentPageParam,
      String(option.data.currentPage + 1)
    );
  }
  history.push({
    search: String(urlSearchParams),
  });
};
const getProjectsOptionsFromUrl = (): IProjectsOptions => {
  const urlSearchParams = new URLSearchParams(window.location.search);
  const paginationCurrentPage = (() => {
    const pageFromUrl = urlSearchParams.get(URLPaginationCurrentPageParam);
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

export const selectProjectForDeleting = (
  id: string
): ISelectProjectForDeleting => ({
  type: selectProjectForDeletingActionType.SELECT_PROJECT_FOR_DELETING,
  payload: { id },
});

export const unselectProjectForDeleting = (
  id: string
): IUnselectProjectForDeleting => ({
  type: unselectProjectForDeletingActionType.UNSELECT_PROJECT_FOR_DELETING,
  payload: { id },
});

export const resetProjectsForDeleting = (): IResetProjectsForDeleting => ({
  type: resetProjectsForDeletingActionType.RESET_PROJECTS_FOR_DELETING,
});

export const deleteProjects = (
  ids: string[],
  workspaceName: IWorkspace['name']
): ActionResult<void, IDeleteProjectsActions> => async (
  dispatch,
  getState,
  { ServiceFactory }
) => {
  dispatch(action(deleteProjectsActionTypes.REQUEST, { ids }));

  await ServiceFactory.getProjectsService()
    .deleteProjects(ids)
    .then(() => {
      const prevPagination = selectProjectsPagination(getState());
      dispatch(action(deleteProjectsActionTypes.SUCCESS, { ids }));
      handleDeleteEntities({
        prevPagination,
        currentPagination: selectProjectsPagination(getState()),
        changePagination: changeProjectsPagination,
        dispatch,
        loadEntities: () =>
          loadProjects(selectCurrentContextFilters(getState()), workspaceName),
      });
    })
    .catch(error => {
      dispatch(
        action(deleteProjectsActionTypes.FAILURE, normalizeError(error))
      );
    });
};

export const loadProjectDatasets = (
  projectId: string
): ActionResult<void, ILoadProjectDatasetsActions> => async (
  dispatch,
  _,
  { ServiceFactory }
) => {
  dispatch(action(loadProjectDatasetsActionTypes.REQUEST, { projectId }));

  await ServiceFactory.getDatasetsService()
    .loadProjectDatasets(projectId)
    .then(datasets => {
      dispatch(
        action(loadProjectDatasetsActionTypes.SUCCESS, { projectId, datasets })
      );
    })
    .catch(error => {
      dispatch(
        action(loadProjectDatasetsActionTypes.FAILURE, {
          projectId,
          error: normalizeError(error),
        })
      );
    });
};

export const setProjects = createAction('@@projects/setProjects')<Project[]>();
