import { matchPath } from 'react-router-dom';

import { IWorkspace } from 'shared/models/Workspace';

import makeRoute, { IRoute } from 'shared/routes/makeRoute';
import * as P from 'shared/routes/pathBuilder';

const workspacePath = '/:workspaceName';

interface IRecordWithWorkspaceName {
  workspaceName: IWorkspace['name'];
}

export type IRouteWithWorkspace<T, B = undefined> = IRoute<
  T & IRecordWithWorkspaceName,
  B
> & {
  getRedirectPathWithCurrentWorkspace: (
    params: Omit<T, 'workspaceName'>
  ) => string;
  withWorkspace: true;
};

export const parseCurrentWorkspaceName = () => {
  const match = matchPath<IRecordWithWorkspaceName>(window.location.pathname, {
    path: workspacePath,
    exact: false,
  });

  return match ? match.params.workspaceName : null;
};

export const makeRouteWithWorkspace = <T extends P.IPath<any, any>>({
  getPath,
}: {
  getPath: () => T;
}): IRouteWithWorkspace<P.GetParams<T>, P.GetQueryParams<T>> => {
  type Params = P.GetParams<T>;
  type QueryParams = P.GetQueryParams<T>;
  const route = makeRoute<IRecordWithWorkspaceName & Params, QueryParams>({
    getPath: () => `${workspacePath}${getPath().value}`,
    allowedUserType: 'authorized',
  });

  const resRoute: IRouteWithWorkspace<Params, QueryParams> = {
    ...route,
    withWorkspace: true,
    getRedirectPathWithCurrentWorkspace: (
      paramsWithoutCurrentWorkspaceName: Omit<Params, 'workspaceName'>
    ) => {
      const currentWorkspaceName = parseCurrentWorkspaceName();
      return route.getRedirectPath({
        ...paramsWithoutCurrentWorkspaceName,
        ...(currentWorkspaceName
          ? ({ workspaceName: currentWorkspaceName } as any)
          : {}),
      });
    },
  };

  return resRoute;
};

export const isRouteWithWorkspace = (route: IRoute<any, any>) => {
  const withWorkspaceKey: keyof IRouteWithWorkspace<any> = 'withWorkspace';
  return withWorkspaceKey in route;
};
