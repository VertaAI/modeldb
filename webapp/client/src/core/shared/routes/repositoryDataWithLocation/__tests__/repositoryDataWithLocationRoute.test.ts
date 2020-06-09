import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  CommitPointerHelpers,
  defaultCommitPointer,
} from 'core/shared/models/Versioning/RepositoryData';
import { repositories } from 'core/shared/utils/tests/mocks/Versioning/repositoriesMocks';
import routes, { GetRouteParams } from 'core/shared/routes';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

const currentWorkspace = userWorkspacesWithCurrentUser.user;
const repository: IRepository = {
  ...repositories[0],
  shortWorkspace: currentWorkspace,
};

const repositoryDataParams: GetRouteParams<typeof routes.repositoryData> = {
  repositoryName: repository.name,
  workspaceName: currentWorkspace.name,
};

describe('(routes repositoryDataWithLocationRoute)', () => {
  describe('getRedirectPath', () => {
    describe('repository root (${repositoryDataRoute}|${repositoryDataRoute}/folder/:commitPointer)', () => {
      describe('${repositoryDataRoute}/data', () => {
        it('should redirect to the root of repository data page when commit points is default branch and data location is not', () => {
          const res = routes.repositoryDataWithLocation.getRedirectPath({
            ...repositoryDataParams,
            location: CommitComponentLocation.makeRoot(),
            commitPointer: defaultCommitPointer,
            type: 'folder',
          });

          expect(res).toEqual(
            routes.repositoryData.getRedirectPath(repositoryDataParams)
          );
        });
      });

      describe('${repositoryDataRoute}/data/folder/:commitPointer', () => {
        it('should redirect to the `${repositoryDataRoute}/folder/:commitPointer` when there are tag and data location is not', () => {
          const res = routes.repositoryDataWithLocation.getRedirectPath({
            ...repositoryDataParams,
            location: CommitComponentLocation.makeRoot(),
            commitPointer: CommitPointerHelpers.makeFromTag('tag'),
            type: 'folder',
          });

          expect(res).toEqual(
            `${routes.repositoryData.getRedirectPath(
              repositoryDataParams
            )}/folder/tag`
          );
        });

        it('should redirect to the `${repositoryDataRoute}/folder/:commitPointer` when there are branch and data location is not', () => {
          const res = routes.repositoryDataWithLocation.getRedirectPath({
            ...repositoryDataParams,
            location: CommitComponentLocation.makeRoot(),
            commitPointer: CommitPointerHelpers.makeFromBranch('branch'),
            type: 'folder',
          });

          expect(res).toEqual(
            `${routes.repositoryData.getRedirectPath(
              repositoryDataParams
            )}/folder/branch`
          );
        });
      });
    });

    describe('in a folder or a blob (`${repositoryDataRoute}/:dataType/:commitPointer/:location`)', () => {
      it('should redirect to `${repositoryDataRoute}/folder/:commit-pointer/:location` for default branch and location', () => {
        const locationPathname = 'subfolder/subfolder2';
        const res = routes.repositoryDataWithLocation.getRedirectPath({
          ...repositoryDataParams,
          location: CommitComponentLocation.makeFromPathname(locationPathname),
          commitPointer: defaultCommitPointer,
          type: 'folder',
        });

        expect(res).toEqual(
          `${routes.repositoryData.getRedirectPath(
            repositoryDataParams
          )}/folder/${defaultCommitPointer.value}/${locationPathname}`
        );
      });

      it('should redirect to `${repositoryDataRoute}/folder/:commit-pointer/:location` for tag and location', () => {
        const locationPathname = 'subfolder/subfolder2';
        const res = routes.repositoryDataWithLocation.getRedirectPath({
          ...repositoryDataParams,
          location: CommitComponentLocation.makeFromPathname(locationPathname),
          commitPointer: CommitPointerHelpers.makeFromTag('tag'),
          type: 'folder',
        });

        expect(res).toEqual(
          `${routes.repositoryData.getRedirectPath(
            repositoryDataParams
          )}/folder/tag/${locationPathname}`
        );
      });

      it('should redirect to `${repositoryDataRoute}/folder/:commit-pointer/:location` for non default branch and location', () => {
        const locationPathname = 'subfolder/subfolder2';
        const res = routes.repositoryDataWithLocation.getRedirectPath({
          ...repositoryDataParams,
          location: CommitComponentLocation.makeFromPathname(locationPathname),
          commitPointer: CommitPointerHelpers.makeFromBranch('branch'),
          type: 'folder',
        });

        expect(res).toEqual(
          `${routes.repositoryData.getRedirectPath(
            repositoryDataParams
          )}/folder/branch/${locationPathname}`
        );
      });
    });
  });
});
