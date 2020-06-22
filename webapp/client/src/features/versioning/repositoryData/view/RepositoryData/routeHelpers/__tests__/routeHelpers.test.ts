import * as CommitComponentLocation from 'shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'shared/models/Versioning/Repository';
import {
  IFullCommitComponentLocationComponents,
  CommitPointerHelpers,
  defaultCommitPointer,
} from 'shared/models/Versioning/RepositoryData';
import { repositories } from 'shared/utils/tests/mocks/models/Versioning/repositoriesMocks';
import routes, { GetRouteParams } from 'shared/routes';
import { userWorkspacesWithCurrentUser } from 'shared/utils/tests/mocks/models/workspace';

import * as RouteHelpers from '../index';

const currentWorkspace = userWorkspacesWithCurrentUser.user;
const repository: IRepository = {
  ...repositories[0],
  shortWorkspace: currentWorkspace,
};

const repositoryDataParams: GetRouteParams<typeof routes.repositoryData> = {
  repositoryName: repository.name,
  workspaceName: currentWorkspace.name,
};

describe('(feature repositoryData) routeHelpers', () => {
  describe('parseFullCommitComponentLocationComponentsFromPathname', () => {
    describe('repository root (${repositoryDataRoute}|${repositoryDataRoute}/folder/:commitPointer)', () => {
      it('should set commit pointer to default branch when URL doesn`t have a commit pointer', () => {
        const pathname = routes.repositoryData.getRedirectPath(
          repositoryDataParams
        );

        const expected: IFullCommitComponentLocationComponents = {
          location: CommitComponentLocation.makeRoot(),
          commitPointer: defaultCommitPointer,
          type: 'folder',
        };
        expect(
          RouteHelpers.parseFullCommitComponentLocationComponentsFromPathname({
            pathname,
            tags: ['tag'],
            branches: ['branch'],
          })
        ).toEqual(expected);
      });

      it('should parse a tag commit pointer when URL is ${repositoryDataRoute}/folder/:tagCommitPointer', () => {
        const targetTag = 'tag';
        const tags = ['adfadf', targetTag];

        const pathname = `${routes.repositoryData.getRedirectPath(
          repositoryDataParams
        )}/folder/${targetTag}`;

        const expected: IFullCommitComponentLocationComponents = {
          location: CommitComponentLocation.makeRoot(),
          commitPointer: CommitPointerHelpers.makeFromTag(targetTag),
          type: 'folder',
        };
        expect(
          RouteHelpers.parseFullCommitComponentLocationComponentsFromPathname({
            pathname,
            tags,
            branches: [],
          })
        ).toEqual(expected);
      });

      it('should parse a branch commit pointer when URL is ${repositoryDataRoute}/folder/:branchCommitPointer', () => {
        const targetBranch = 'branch';
        const branches = ['adfadf', targetBranch];

        const pathname = `${routes.repositoryData.getRedirectPath(
          repositoryDataParams
        )}/folder/${targetBranch}`;

        const expected: IFullCommitComponentLocationComponents = {
          location: CommitComponentLocation.makeRoot(),
          commitPointer: CommitPointerHelpers.makeFromBranch(targetBranch),
          type: 'folder',
        };
        expect(
          RouteHelpers.parseFullCommitComponentLocationComponentsFromPathname({
            pathname,
            branches,
            tags: ['zcv'],
          })
        ).toEqual(expected);
      });

      it('should detect commit pointer as commit sha commit points when URL is ${repositoryDataRoute}/folder/:commitPointer and commit pointer is not branch or tag', () => {
        const pathname = `${routes.repositoryData.getRedirectPath(
          repositoryDataParams
        )}/folder/306ed233429fe6398f759d84a00097a68f7cabaae53b7c90f862e0d1c77914de`;

        const expected: IFullCommitComponentLocationComponents = {
          location: CommitComponentLocation.makeRoot(),
          commitPointer: CommitPointerHelpers.makeFromCommitSha(
            '306ed233429fe6398f759d84a00097a68f7cabaae53b7c90f862e0d1c77914de'
          ),
          type: 'folder',
        };
        expect(
          RouteHelpers.parseFullCommitComponentLocationComponentsFromPathname({
            pathname,
            branches: ['branch-1', 'branch-2'],
            tags: ['tag-1', 'tag-2'],
          })
        ).toEqual(expected);
      });
    });

    describe('in a folder or a blob (`${repositoryDataRoute}/:dataType/:commitPointer/:location`)', () => {
      it('should parse branch commit pointer, data folder type and location when URL is `${repositoryDataRoute}/:dataType/:branchCommitPointer/:location`', () => {
        const targetTag = 'tag';
        const tags = ['adfadf', targetTag];
        const location = 'subfolder1/subfolder2';

        const pathname = `${routes.repositoryData.getRedirectPath(
          repositoryDataParams
        )}/folder/${targetTag}/${location}`;

        const expected: IFullCommitComponentLocationComponents = {
          location: CommitComponentLocation.makeFromPathname(location),
          commitPointer: CommitPointerHelpers.makeFromTag(targetTag),
          type: 'folder',
        };
        expect(
          RouteHelpers.parseFullCommitComponentLocationComponentsFromPathname({
            pathname,
            tags,
            branches: [],
          })
        ).toEqual(expected);
      });

      it('should parse tag commit pointer, data folder type and location when URL is `${repositoryDataRoute}/:dataType/:tagCommitPointer/:location`', () => {
        const targetBranch = 'branch';
        const branches = ['branch-2', targetBranch];
        const location = 'subfolder1/subfolder2';
        const pathname = `${routes.repositoryData.getRedirectPath(
          repositoryDataParams
        )}/folder/${targetBranch}/${location}`;

        const expected: IFullCommitComponentLocationComponents = {
          location: CommitComponentLocation.makeFromPathname(location),
          commitPointer: CommitPointerHelpers.makeFromBranch('branch'),
          type: 'folder',
        };

        expect(
          RouteHelpers.parseFullCommitComponentLocationComponentsFromPathname({
            pathname,
            branches,
            tags: [],
          })
        ).toEqual(expected);
      });
    });
  });
});
