import { ApolloClient } from 'apollo-boost';
import { connectRouter, RouterState } from 'connected-react-router';
import { History } from 'history';
import { Action, AnyAction, combineReducers, Dispatch } from 'redux';
import { ThunkAction } from 'redux-thunk';

import ServiceFactory from 'services/ServiceFactory';

import * as ArtifactManager from 'features/artifactManager/store';
import * as Comment from 'features/comments';
import * as Datasets from 'features/datasets/store';
import * as DatasetVersions from 'features/datasetVersions/store';
import * as CompareModels from 'features/compareModels';
import * as CompareDatasets from 'features/compareDatasets';
import * as DescriptionManager from 'features/descriptionManager';
import * as ExperimentRuns from 'features/experimentRuns/store';
import * as ExperimentRunsTableConfig from 'features/experimentRunsTableConfig';
import * as Experiments from 'features/experiments/store';
import * as Filter from 'features/filter';
import * as HighLevelSearch from 'features/highLevelSearch';
import * as Layout from 'features/layout';
import * as ProjectCreation from 'features/projectCreation/store';
import * as Projects from 'features/projects/store';
import * as TagsManagers from 'features/tagsManager';
import * as RepositoryNavigation from 'features/versioning/repositoryNavigation';
import * as Workspaces from 'features/workspaces/store';

export interface IApplicationState
  extends Filter.IFilterRootState,
  Comment.ICommentsRootState,
  ExperimentRunsTableConfig.IExperimentRunsTableConfigRootState,
  Layout.ILayoutRootState {
  experiments: Experiments.IExperimentsState;
  compareModels: CompareModels.ICompareModelsState;
  compareDatasets: CompareDatasets.ICompareDatasetsState;
  experimentRuns: ExperimentRuns.IExperimentRunsState;
  projectCreation: ProjectCreation.IProjectCreationState;
  projects: Projects.IProjectsState;
  router: RouterState;
  tagsManager: TagsManagers.ITagsManagerState;
  descriptionManager: DescriptionManager.IDescriptionManagerState;
  artifactManager: ArtifactManager.IArtifactManagerState;
  datasets: Datasets.IDatasetsState;
  datasetVersions: DatasetVersions.IDatasetVersionsState;
  workspaces: Workspaces.IWorkspaces;
  repositoryNavigation: RepositoryNavigation.types.IRepositoryNavigationState;
  highLevelSearch: HighLevelSearch.types.IHighLevelSearchState;
}

// Additional props for connected React components. This prop is passed by default with `connect()`
export interface IConnectedReduxProps<A extends Action = any> {
  dispatch: Dispatch<A>;
}

export const createRootReducer = (history: History) =>
  combineReducers<IApplicationState>({
    layout: Layout.reducer,
    experiments: Experiments.reducer,
    comments: Comment.reducer,
    compareModels: CompareModels.reducer,
    compareDatasets: CompareDatasets.reducer,
    experimentRunsTableConfig: ExperimentRunsTableConfig.reducer,
    experimentRuns: ExperimentRuns.reducer,
    filters: Filter.reducer,
    projectCreation: ProjectCreation.reducer,
    projects: Projects.reducer,
    router: connectRouter(history),
    tagsManager: TagsManagers.reducer,
    descriptionManager: DescriptionManager.reducer,
    artifactManager: ArtifactManager.reducer,
    datasets: Datasets.reducer,
    datasetVersions: DatasetVersions.reducer,
    workspaces: Workspaces.reducer,
    repositoryNavigation: RepositoryNavigation.reducer,
    highLevelSearch: HighLevelSearch.reducer,
  });

export interface IThunkActionDependencies
  extends Filter.IThunkActionDependencies {
  ServiceFactory: typeof ServiceFactory;
  history: History;
  apolloClient: ApolloClient<any>;
}

export type ActionResult<R = void, A extends Action = AnyAction> = ThunkAction<
  R,
  IApplicationState,
  IThunkActionDependencies,
  A
>;
