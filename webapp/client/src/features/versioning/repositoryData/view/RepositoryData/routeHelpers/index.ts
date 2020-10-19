import * as CommitComponentLocation from 'shared/models/Versioning/CommitComponentLocation';

import { IRepository } from 'shared/models/Versioning/Repository';
import {
  IFullCommitComponentLocationComponents,
  IFolderElement,
  ICommitComponent,
  CommitTag,
  CommitPointerHelpers,
  Branch,
  defaultCommitPointer,
} from 'shared/models/Versioning/RepositoryData';
import { IWorkspace } from 'shared/models/Workspace';
import routes, { GetRouteParams } from 'shared/routes';
import { IRepositoryDataWithLocationParams } from 'shared/routes/repositoryDataWithLocation';

export type Options = Omit<IFullCommitComponentLocationComponents, 'type'> & {
  repositoryName: IRepository['name'];
  type: IFullCommitComponentLocationComponents['type'] | null;
  workspaceName: IWorkspace['name'];
};

export function addName(
  name: IFolderElement['name'],
  type: ICommitComponent['type'],
  currentLocationOptions: Options
) {
  return routes.repositoryDataWithLocation.getRedirectPath({
    ...currentLocationOptions,
    type,
    location: CommitComponentLocation.add(
      name,
      currentLocationOptions.location
    ),
  });
}
export function goBack(currentLocationOptions: Options) {
  return routes.repositoryDataWithLocation.getRedirectPath({
    ...currentLocationOptions,
    type: 'folder',
    location: currentLocationOptions.location.slice(0, -1),
  });
}

export type RepositoryDataParams = Partial<IRepositoryDataWithLocationParams> &
  GetRouteParams<typeof routes.repositoryData>;

export const parseFullCommitComponentLocationComponentsFromPathname = ({
  pathname,
  tags,
  branches,
}: {
  pathname: string;
  tags: CommitTag[];
  branches: Branch[];
}): IFullCommitComponentLocationComponents => {
  const match = (routes.repositoryDataWithLocation.getMatch(pathname) ||
    routes.repositoryData.getMatch(pathname)) as RepositoryDataParams | null;

  if (!match) {
    throw new Error('match is not defined!');
  }

  const location = CommitComponentLocation.makeFromPathname(
    match.locationPathname || ''
  );
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
