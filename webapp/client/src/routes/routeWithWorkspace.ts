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
};

type IMakeRouteWithWorkspaceSettings<T, B = undefined> = Omit<
  IRouteSettings<T & IRecordWithWorkspaceName, B>,
  'allowedUserType'
>;

export const makeRouteWithWorkspace = <T, B = undefined>(
  settings: IMakeRouteWithWorkspaceSettings<T, B>
): IRouteWithWorkspace<T, B> => {
  const route = makeRoute<IRecordWithWorkspaceName & T, B>({
    getPath: () => `${workspacePath}${settings.getPath()}`,
  });

  const resRoute: IRouteWithWorkspace<T, B> = {
    ...route,
    getRedirectPathWithCurrentWorkspace: (
      paramsWithoutCurrentWorkspaceName: Omit<T, 'workspaceName'>
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
          ? ({ workspaceName: match.params.workspaceName || 'personal' } as any)
          : { workspaceName: 'personal' }),
      });
    },
  };

  return resRoute;
};
