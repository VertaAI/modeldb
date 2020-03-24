import * as DataLocation from 'core/shared/models/Versioning/DataLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  IFullDataLocationComponents,
  CommitPointerHelpers,
  defaultCommitPointer,
} from 'core/shared/models/Versioning/RepositoryData';
import routes, { GetRouteParams } from 'routes';
import { repositories } from 'core/shared/utils/tests/mocks/Versioning/repositoriesMocks';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

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

const getRedirectPath = (options: RouteHelpers.Options) => {
  delete (global as any).window.location;
  (global as any).window = Object.create(window);
  (global as any).window.location = {
    pathname: routes.repositoryData.getRedirectPath({
      repositoryName: repository.name,
      workspaceName: userWorkspacesWithCurrentUser.user.name,
    }),
  };
  return RouteHelpers.getRedirectPath(options);
};

describe('(feature repositoryData) routeHelpers', () => {
  describe('getRedirectPath', () => {
    describe('repository root (${repositoryDataRoute}|${repositoryDataRoute}/folder/:commitPointer)', () => {
      describe('${repositoryDataRoute}/data', () => {
        it('should redirect to the root of repository data page when commit points is default branch and data location is not', () => {
          const res = getRedirectPath({
            ...repositoryDataParams,
            location: DataLocation.makeRoot(),
            commitPointer: defaultCommitPointer,
            type: null,
          });

          expect(res).toEqual(
            routes.repositoryData.getRedirectPath(repositoryDataParams)
          );
        });
      });

      describe('${repositoryDataRoute}/data/folder/:commitPointer', () => {
        it('should redirect to the `${repositoryDataRoute}/folder/:commitPointer` when there are tag and data location is not', () => {
          const res = getRedirectPath({
            ...repositoryDataParams,
            location: DataLocation.makeRoot(),
            commitPointer: CommitPointerHelpers.makeFromTag('tag'),
            type: null,
          });

          expect(res).toEqual(
            `${routes.repositoryData.getRedirectPath(
              repositoryDataParams
            )}/folder/tag`
          );
        });

        it('should redirect to the `${repositoryDataRoute}/folder/:commitPointer` when there are branch and data location is not', () => {
          const res = getRedirectPath({
            ...repositoryDataParams,
            location: DataLocation.makeRoot(),
            commitPointer: CommitPointerHelpers.makeFromBranch('branch'),
            type: null,
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
        const res = getRedirectPath({
          ...repositoryDataParams,
          location: DataLocation.makeFromPathname(locationPathname),
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
        const res = getRedirectPath({
          ...repositoryDataParams,
          location: DataLocation.makeFromPathname(locationPathname),
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
        const res = getRedirectPath({
          ...repositoryDataParams,
          location: DataLocation.makeFromPathname(locationPathname),
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

    it('should redirect to `${repositoryDataRoute}` when location is not empty and data type is empty', () => {
      const res = getRedirectPath({
        ...repositoryDataParams,
        location: DataLocation.makeFromNames([
          'folder' as any,
          'subfolder' as any,
        ]),
        commitPointer: defaultCommitPointer,
        type: null,
      });

      expect(res).toEqual(
        routes.repositoryData.getRedirectPath(repositoryDataParams)
      );
    });
  });

  describe('parseFullDataLocationComponentsFromPathname', () => {
    describe('repository root (${repositoryDataRoute}|${repositoryDataRoute}/folder/:commitPointer)', () => {
      it('should set commit pointer to default branch when URL doesn`t have a commit pointer', () => {
        const pathname = routes.repositoryData.getRedirectPath(
          repositoryDataParams
        );

        const expected: IFullDataLocationComponents = {
          location: DataLocation.makeRoot(),
          commitPointer: defaultCommitPointer,
          type: 'folder',
        };
        expect(
          RouteHelpers.parseFullDataLocationComponentsFromPathname({
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

        const expected: IFullDataLocationComponents = {
          location: DataLocation.makeRoot(),
          commitPointer: CommitPointerHelpers.makeFromTag(targetTag),
          type: 'folder',
        };
        expect(
          RouteHelpers.parseFullDataLocationComponentsFromPathname({
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

        const expected: IFullDataLocationComponents = {
          location: DataLocation.makeRoot(),
          commitPointer: CommitPointerHelpers.makeFromBranch(targetBranch),
          type: 'folder',
        };
        expect(
          RouteHelpers.parseFullDataLocationComponentsFromPathname({
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

        const expected: IFullDataLocationComponents = {
          location: DataLocation.makeRoot(),
          commitPointer: CommitPointerHelpers.makeFromCommitSha(
            '306ed233429fe6398f759d84a00097a68f7cabaae53b7c90f862e0d1c77914de'
          ),
          type: 'folder',
        };
        expect(
          RouteHelpers.parseFullDataLocationComponentsFromPathname({
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

        const expected: IFullDataLocationComponents = {
          location: DataLocation.makeFromPathname(location),
          commitPointer: CommitPointerHelpers.makeFromTag(targetTag),
          type: 'folder',
        };
        expect(
          RouteHelpers.parseFullDataLocationComponentsFromPathname({
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

        const expected: IFullDataLocationComponents = {
          location: DataLocation.makeFromPathname(location),
          commitPointer: CommitPointerHelpers.makeFromBranch('branch'),
          type: 'folder',
        };

        expect(
          RouteHelpers.parseFullDataLocationComponentsFromPathname({
            pathname,
            branches,
            tags: [],
          })
        ).toEqual(expected);
      });
    });
  });
});
