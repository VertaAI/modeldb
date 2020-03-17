import * as DataLocation from 'core/shared/models/Versioning/DataLocation';
import * as R from 'ramda';

import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  IFullDataLocationComponents,
  IFolderElement,
  IRepositoryData,
  CommitTag,
  CommitPointerHelpers,
  Branch,
  defaultCommitPointer,
} from 'core/shared/models/Versioning/RepositoryData';
import routes, { GetRouteParams } from 'routes';

export type Options = Omit<IFullDataLocationComponents, 'type'> & {
  repositoryName: IRepository['name'];
  type: IFullDataLocationComponents['type'] | null;
};
export function getRedirectPath({
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
export function addName(
  name: IFolderElement['name'],
  type: IRepositoryData['type'],
  currentLocationOptions: Options
) {
  return getRedirectPath({
    ...currentLocationOptions,
    type,
    location: DataLocation.add(name, currentLocationOptions.location),
  });
}
export function goBack(currentLocationOptions: Options) {
  return getRedirectPath({
    ...currentLocationOptions,
    type: 'folder',
    location: currentLocationOptions.location.slice(0, -1),
  });
}

export type RepositoryDataParams = Partial<
  GetRouteParams<typeof routes.repositoryDataWithLocation>
> &
  GetRouteParams<typeof routes.repositoryData>;

export const parseFullDataLocationComponentsFromPathname = ({
  pathname,
  tags,
  branches,
}: {
  pathname: string;
  tags: CommitTag[];
  branches: Branch[];
}): IFullDataLocationComponents => {
  const match = (routes.repositoryDataWithLocation.getMatch(pathname) ||
    routes.repositoryData.getMatch(pathname)) as RepositoryDataParams | null;

  if (!match) {
    throw new Error('match is not defined!');
  }

  const location = DataLocation.makeFromPathname(match.locationPathname || '');
  return {
    location,
    type: match.dataType || 'folder',
    commitPointer: !match.commitPointerValue
      ? defaultCommitPointer
      : CommitPointerHelpers.makeCommitPointerFromString(
          match.commitPointerValue,
          { branches, tags }
        ),
  };
};
