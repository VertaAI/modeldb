import * as DataLocation from 'core/shared/models/Repository/DataLocation';
import * as R from 'ramda';

import { IRepository } from 'core/shared/models/Repository/Repository';
import {
  IFullDataLocationComponents,
  IFolderElement,
  IRepositoryData,
  CommitTag,
  CommitPointerHelpers,
  Branch,
  defaultCommitPointer,
} from 'core/shared/models/Repository/RepositoryData';
import routes, { GetRouteParams } from 'routes';

export type Options = Omit<IFullDataLocationComponents, 'type'> & {
  repositoryName: IRepository['name'];
  type: IFullDataLocationComponents['type'] | null;
};
export function getRedirectPathToRepositoryDataPage({
  commitPointer,
  location,
  type,
  repositoryName,
}: Options) {
  if (!R.equals(commitPointer, defaultCommitPointer) && location.length === 0) {
    return routes.repositoryDataWithLocation.getRedirectPathWithCurrentWorkspace(
      {
        repositoryName,
        dataType: 'folder',
        locationPathname: '',
        commitPointerValue: commitPointer.value,
      }
    );
  }

  if (location.length === 0 || !type) {
    return routes.repositoryData.getRedirectPathWithCurrentWorkspace({
      repositoryName,
    });
  }

  return routes.repositoryDataWithLocation.getRedirectPathWithCurrentWorkspace({
    repositoryName,
    dataType: type,
    locationPathname: DataLocation.toPathname(location),
    commitPointerValue: commitPointer.value,
  });
}
