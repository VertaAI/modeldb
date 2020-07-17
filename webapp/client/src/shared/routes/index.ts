import { ArgumentTypes } from 'shared/utils/types';

import { IRepository } from 'shared/models/Versioning/Repository';
import {
  CommitPointer,
  ICommit,
} from 'shared/models/Versioning/RepositoryData';
import { makeRouteFromPath, IRoute as _IRoute } from 'shared/routes/makeRoute';
import * as P from 'shared/routes/pathBuilder';
import { IURLWithFilters } from 'shared/models/Filters';
import { IURLWithResultsSorting } from 'features/highLevelSearch/url';

import {
  isRouteWithWorkspace,
  makeRouteWithWorkspace,
} from './routeWithWorkspace';
import { makeRepositoryDataWithLocationRoute } from './repositoryDataWithLocation';

export type IRoute<T, B = undefined> = _IRoute<T, B>;

export type RoutesWithWorkspaces =
  | 'projects'
  | 'datasets'
  | 'repositories'
  | 'highLevelSearch';

const routes = {
  index: makeRouteFromPath({
    getPath: () => P.makePath()(),
    allowedUserType: 'any',
  }),

  workspace: makeRouteWithWorkspace({
    getPath: () => P.makePath()(),
  }),

  highLevelSearch: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath('search')<
        { q?: string; type?: string; page?: string } & IURLWithResultsSorting
      >(),
  }),

  datasets: makeRouteWithWorkspace({
    getPath: () => P.makePath('datasets')(),
  }),
  datasetCreation: makeRouteWithWorkspace({
    getPath: () => P.makePath('datasets', 'new')(),
  }),
  datasetSummary: makeRouteWithWorkspace({
    getPath: () => P.makePath('datasets', P.param('datasetId')(), 'summary')(),
  }),
  datasetVersions: makeRouteWithWorkspace({
    getPath: () => P.makePath('datasets', P.param('datasetId')(), 'versions')(),
  }),
  datasetVersion: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath(
        'datasets',
        P.param('datasetId')(),
        'versions',
        P.param('datasetVersionId')()
      )(),
  }),
  datasetSettings: makeRouteWithWorkspace({
    getPath: () => P.makePath('datasets', P.param('datasetId')(), 'settings')(),
  }),
  compareDatasetVersions: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath(
        'datasets',
        P.param('datasetId')(),
        'versions',
        'compare',
        P.param('datasetVersionId1')(),
        P.param('datasetVersionId2')()
      )(),
  }),

  projects: makeRouteWithWorkspace({
    getPath: () => P.makePath('projects')(),
  }),
  project: makeRouteWithWorkspace({
    getPath: () => P.makePath('projects', P.param('projectId')())(),
  }),
  projectCreation: makeRouteWithWorkspace({
    getPath: () => P.makePath('projects', 'new')(),
  }),
  projectSummary: makeRouteWithWorkspace({
    getPath: () => P.makePath('projects', P.param('projectId')(), 'summary')(),
  }),
  projectSettings: makeRouteWithWorkspace({
    getPath: () => P.makePath('projects', P.param('projectId')(), 'settings')(),
  }),
  experimentRuns: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath('projects', P.param('projectId')(), 'exp-runs')<
        IURLWithFilters
      >(),
  }),
  charts: makeRouteWithWorkspace({
    getPath: () => P.makePath('projects', P.param('projectId')(), 'charts')(),
  }),
  experiments: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath('projects', P.param('projectId')(), 'experiments')(),
  }),
  experimentCreation: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath('projects', P.param('projectId')(), 'experiments', 'new')(),
  }),
  modelRecord: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath(
        'projects',
        P.param('projectId')(),
        'exp-runs',
        P.param('modelRecordId')()
      )(),
  }),
  compareModels: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath(
        'projects',
        P.param('projectId')(),
        'exp-runs',
        'compare',
        P.paramWithMod('modelIds', '*')<{
          modelIds?: string
        }>(),
      )(),
  }),

  repositories: makeRouteWithWorkspace({
    getPath: () => P.makePath('repositories')<any>(),
  }),
  createRepository: makeRouteWithWorkspace({
    getPath: () => P.makePath('repositories', 'new')(),
  }),
  repository: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath(
        'repositories',
        P.param('repositoryName')<IRepository['name']>()
      )(),
  }),
  repositoryData: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath(
        'repositories',
        P.param('repositoryName')<IRepository['name']>(),
        'data'
      )(),
  }),
  repositoryDataWithLocation: makeRepositoryDataWithLocationRoute(),
  repositoryCommitsHistory: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath(
        'repositories',
        P.param('repositoryName')<IRepository['name']>(),
        'data',
        'commits',
        P.param('commitPointerValue')<CommitPointer['value']>(),
        P.paramWithMod('locationPathname', '*')<{
          locationPathname?: string;
        }>()
      )<{ page?: string }>(),
  }),
  repositoryCommit: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath(
        'repositories',
        P.param('repositoryName')<IRepository['name']>(),
        'data',
        'commit',
        P.param('commitSha')<ICommit['sha']>()
      )(),
  }),
  repositorySettings: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath(
        'repositories',
        P.param('repositoryName')<IRepository['name']>(),
        'settings'
      )(),
  }),
  repositoryCompareChanges: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath(
        'repositories',
        P.param('repositoryName')<IRepository['name']>(),
        'data',
        'compare',
        P.param('commitPointerAValue')<CommitPointer['value']>(),
        P.param('commitPointerBValue')<CommitPointer['value']>()
      )(),
  }),
  repositoryNetworkGraph: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath(
        'repositories',
        P.param('repositoryName')<IRepository['name']>(),
        'network'
      )(),
  }),
  repositoryMergeConflicts: makeRouteWithWorkspace({
    getPath: () =>
      P.makePath(
        'repositories',
        P.param('repositoryName')<IRepository['name']>(),
        'data',
        'conflicts',
        P.param('commitPointerAValue')<CommitPointer['value']>(),
        P.param('commitPointerBValue')<CommitPointer['value']>()
      )(),
  }),
};

export const findAppRouteByPathname = (
  pathname: string,
  appRoutes: typeof routes
) => {
  const { workspace, ...routesWithoutWorkspace } = appRoutes;

  const route = Object.values(routesWithoutWorkspace).find(route_ =>
    Boolean(route_.getMatch(pathname, true))
  );

  if (route) {
    return route;
  }

  return workspace.getMatch(pathname, true) ? workspace : undefined;
};

export const checkIsAppPathnameWithoutWorkspace = (
  pathname: string,
  appRoutes: typeof routes
) => {
  const route = findAppRouteByPathname(pathname, appRoutes);
  return Boolean(route && !isRouteWithWorkspace(route));
};

export type GetRouteParams<T extends IRoute<any, any>> = ArgumentTypes<
  T['getRedirectPath']
>[0];
export type GetRouteQueryParams<
  T extends _IRoute<any, any>
> = T extends _IRoute<any, infer QueryParams> ? QueryParams : {};
export default routes;
