import * as R from 'ramda';
import { generatePath } from 'react-router';

import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  CommitPointer,
  ICommitComponent,
  IFullCommitComponentLocationComponents,
  defaultCommitPointer,
} from 'core/shared/models/Versioning/RepositoryData';
import makeRoute from 'core/shared/routes/makeRoute';
import * as P from 'core/shared/routes/pathBuilder';
import { IWorkspace } from 'core/shared/models/Workspace';
import routes from 'routes';
import { parseCurrentWorkspaceName } from 'routes/routeWithWorkspace';

const routePath = P.makePath(
  P.param('workspaceName')<IWorkspace['name']>(),
  'repositories',
  P.param('repositoryName')<IRepository['name']>(),
  'data',
  P.orParam('dataType')<ICommitComponent['type']>(['blob', 'folder']),
  P.param('commitPointerValue')<CommitPointer['value']>(),
  P.paramWithMod('locationPathname', '*')<{
    locationPathname?: string;
  }>()
)();

export type IRepositoryDataWithLocationParams = P.GetParams<typeof routePath>;

interface GetRedirectPathOptions
  extends IFullCommitComponentLocationComponents {
  repositoryName: IRepository['name'];
  workspaceName: IWorkspace['name'];
}

export const makeRepositoryDataWithLocationRoute = () => {
  const route = makeRoute<
    IRepositoryDataWithLocationParams,
    {},
    GetRedirectPathOptions
  >({
    getPath: () => routePath.value,
    allowedUserType: 'authorized',
  });

  return {
    ...route,
    getRedirectPath,
    getRedirectPathWithCurrentWorkspace,
    withWorkspace: true as true,
  };
};

const getRedirectPathWithCurrentWorkspace = (
  options: Omit<GetRedirectPathOptions, 'workspaceName'>
) => {
  const currentWorkspaceName = parseCurrentWorkspaceName();

  if (currentWorkspaceName) {
    getRedirectPath({ ...options, workspaceName: currentWorkspaceName });
  }

  throw new Error('Can`t parse "currentWorkspaceName"');
};

const getRedirectPath = (options: GetRedirectPathOptions) => {
  const {
    commitPointer,
    repositoryName,
    location,
    type,
    workspaceName,
  } = options;

  if (
    options.location.length === 0 &&
    R.equals(options.commitPointer, defaultCommitPointer)
  ) {
    return routes.repositoryData.getRedirectPath({
      repositoryName,
      workspaceName,
    });
  }

  const params: Omit<IRepositoryDataWithLocationParams, 'locationPathname'> = {
    repositoryName,
    workspaceName,
    dataType: type,
    commitPointerValue: commitPointer.value,
  };

  if (location.length === 0) {
    return generatePath(routePath.value, { ...params });
  }

  return `${generatePath(routePath.value, {
    ...params,
  })}/${CommitComponentLocation.toPathname(location)}`;
};
