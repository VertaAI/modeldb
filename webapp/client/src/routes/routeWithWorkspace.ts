import { matchPath } from 'react-router-dom';

import { IWorkspace } from 'models/Workspace';

import makeRoute, {
  IRoute,
  IRouteSettings,
} from 'core/shared/routes/makeRoute';

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

type IMakeRouteWithWorkspaceSettings<T, B = undefined> = Omit<
  IRouteSettings<T & IRecordWithWorkspaceName, B>,
  'allowedUserType'
>;

export const makeRouteWithWorkspace = <Params, QueryParams = undefined>(
  settings: IMakeRouteWithWorkspaceSettings<Params, QueryParams>
): IRouteWithWorkspace<Params, QueryParams> => {
  const route = makeRoute<IRecordWithWorkspaceName & Params, QueryParams>({
    getPath: () => `${workspacePath}${settings.getPath()}`,
  });

  const resRoute: IRouteWithWorkspace<Params, QueryParams> = {
    ...route,
    withWorkspace: true,
    getRedirectPathWithCurrentWorkspace: (
      paramsWithoutCurrentWorkspaceName: Omit<Params, 'workspaceName'>
    ) => {
      const match = matchPath<IRecordWithWorkspaceName>(
        window.location.pathname,
        {
          path: workspacePath,
          exact: false,
        }
      );
      return route.getRedirectPath({
        ...paramsWithoutCurrentWorkspaceName,
        ...(match && match.params
          ? ({ workspaceName: match.params.workspaceName } as any)
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
