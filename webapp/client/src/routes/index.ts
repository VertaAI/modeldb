import makeRoute, { IRoute as _IRoute } from 'core/shared/routes/makeRoute';
import { ArgumentTypes } from 'core/shared/utils/types';

import { makeRouteWithWorkspace } from './routeWithWorkspace';

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
};

export type GetRouteParams<T extends IRoute<any, any>> = ArgumentTypes<
  T['getRedirectPath']
>[0];
export type GetRouteQueryParams<
  T extends _IRoute<any, any>
> = T extends _IRoute<any, infer QueryParams> ? QueryParams : {};
export default routes;
