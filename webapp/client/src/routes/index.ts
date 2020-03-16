import { ArgumentTypes } from 'core/shared/utils/types';

import { IRepository } from 'core/shared/models/Repository/Repository';
import {
  IRepositoryData,
  SHA,
  CommitPointer,
  ICommit,
} from 'core/shared/models/Repository/RepositoryData';
import makeRoute, { IRoute as _IRoute } from 'core/shared/routes/makeRoute';

import {
  makeRouteWithWorkspace,
  isRouteWithWorkspace,
} from './routeWithWorkspace';

export type IRoute<T, B = undefined> = _IRoute<T, B>;

export type RoutesWithWorkspaces = 'projects' | 'datasets';

const routes = {
  index: makeRoute({
    getPath: () => '/',
  }),

  workspace: makeRouteWithWorkspace({
    getPath: () => '/',
  }),

  datasets: makeRouteWithWorkspace({
    getPath: () => '/datasets',
  }),
  datasetCreation: makeRouteWithWorkspace({
    getPath: () => '/datasets/new',
  }),
  datasetSummary: makeRouteWithWorkspace<{
    datasetId: string;
  }>({
    getPath: () => '/datasets/:datasetId/summary',
  }),
  datasetVersions: makeRouteWithWorkspace<{
    datasetId: string;
  }>({
    getPath: () => '/datasets/:datasetId/versions',
  }),
  datasetVersion: makeRouteWithWorkspace<{
    datasetId: string;
    datasetVersionId: string;
  }>({
    getPath: () => '/datasets/:datasetId/versions/:datasetVersionId',
  }),
  compareDatasetVersions: makeRouteWithWorkspace<{
    datasetId: string;
    datasetVersionId1: string;
    datasetVersionId2: string;
  }>({
    getPath: () =>
      '/datasets/:datasetId/versions/compare/:datasetVersionId1/:datasetVersionId2',
  }),

  projects: makeRouteWithWorkspace({
    getPath: () => `/projects`,
  }),
  projectCreation: makeRouteWithWorkspace({
    getPath: () => '/projects/new',
  }),
  projectSummary: makeRouteWithWorkspace<{
    projectId: string;
  }>({
    getPath: () => `/projects/:projectId/summary`,
  }),
  experimentRuns: makeRouteWithWorkspace<{
    projectId: string;
  }>({
    getPath: () => '/projects/:projectId/exp-runs',
  }),
  charts: makeRouteWithWorkspace<{
    projectId: string;
  }>({
    getPath: () => '/projects/:projectId/charts',
  }),
  experiments: makeRouteWithWorkspace<{
    projectId: string;
  }>({
    getPath: () => '/projects/:projectId/experiments',
  }),
  experimentCreation: makeRouteWithWorkspace<{
    projectId: string;
  }>({
    getPath: () => '/projects/:projectId/experiments/new',
  }),
  modelRecord: makeRouteWithWorkspace<{
    projectId: string;
    modelRecordId: string;
  }>({
    getPath: () => '/projects/:projectId/exp-runs/:modelRecordId',
  }),
  compareModels: makeRouteWithWorkspace<{
    projectId: string;
    modelRecordId1: string;
    modelRecordId2: string;
  }>({
    getPath: () =>
      '/projects/:projectId/exp-runs/compare/:modelRecordId1/:modelRecordId2',
  }),

  repositories: makeRouteWithWorkspace<{}, { page: string }>({
    getPath: () => '/repositories',
  }),
  createRepository: makeRouteWithWorkspace({
    getPath: () => '/repositories/new',
  }),
  repositoryData: makeRouteWithWorkspace<{
    repositoryName: IRepository['name'];
  }>({
    getPath: () => `/repositories/:repositoryName/data`,
  }),
  repositoryCommitsHistory: makeRouteWithWorkspace<
    {
      repositoryName: IRepository['name'];
      commitPointerValue: CommitPointer['value'];
      locationPathname?: string;
    },
    { page?: string }
  >({
    getPath: () =>
      `/repositories/:repositoryName/data/commits/:commitPointerValue/:locationPathname*`,
  }),
  repositoryCommit: makeRouteWithWorkspace<{
    commitSha: ICommit['sha'];
    repositoryName: IRepository['name'];
  }>({
    getPath: () => `/repositories/:repositoryName/data/commit/:commitSha`,
  }),
  repositoryDataWithLocation: (() => {
    const route = makeRouteWithWorkspace<{
      repositoryName: IRepository['name'];
      dataType: IRepositoryData['type'];
      commitPointerValue: CommitPointer['value'];
      locationPathname?: string;
    }>({
      getPath: () =>
        `/repositories/:repositoryName/data/:dataType(blob|folder)/:commitPointerValue/:locationPathname*`,
    });

    return {
      ...route,
      getRedirectPath: ({ locationPathname, ...restParams }) => {
        return `${route.getRedirectPath(restParams)}${
          locationPathname ? `/${locationPathname}` : ''
        }`;
      },
      getRedirectPathWithCurrentWorkspace: ({
        locationPathname,
        ...restParams
      }) => {
        return `${route.getRedirectPathWithCurrentWorkspace(restParams)}${
          locationPathname ? `/${locationPathname}` : ''
        }`;
      },
    } as typeof route;
  })(),
  repositorySettings: makeRouteWithWorkspace<{
    repositoryName: IRepository['name'];
  }>({
    getPath: () => `/repositories/:repositoryName/settings`,
  }),
  repositoryCompareChanges: makeRouteWithWorkspace<{
    repositoryName: IRepository['name'];
    commitPointerAValue: CommitPointer['value'] | string;
    commitPointerBValue: CommitPointer['value'] | string;
  }>({
    getPath: () =>
      '/repositories/:repositoryName/data/compare/:commitPointerAValue/:commitPointerBValue',
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
