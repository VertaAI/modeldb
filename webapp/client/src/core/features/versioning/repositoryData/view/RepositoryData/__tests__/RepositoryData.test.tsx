import { ReactWrapper } from 'enzyme';
import * as React from 'react';
import { Switch, Route } from 'react-router';
import routes, { GetRouteParams } from 'routes';

import RepositoryDataService from 'core/services/versioning/repositoryData/RepositoryDataService';
import * as B from 'core/shared/models/Versioning/BuildCommitTree';
import * as DataLocation from 'core/shared/models/Versioning/DataLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  CommitTag,
  IHydratedCommit,
  IFolder,
  IDataRequest,
  IFullDataLocationComponents,
  CommitPointerHelpers,
  defaultCommitPointer,
  defaultBranch,
  ICommitWithData,
} from 'core/shared/models/Versioning/RepositoryData';
import { flushAllPromisesFor } from 'core/shared/utils/tests/integrations/flushAllPromisesFor';
import { makeMockedService } from 'core/shared/utils/tests/integrations/mockServiceMethod';
import { findByText } from 'core/shared/utils/tests/react/helpers';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import makeMountComponentWithPredefinedData from 'utils/tests/integrations/makeMountComponentWithPredefinedData';
import { repositories } from 'core/shared/utils/tests/mocks/Versioning/repositoriesMocks';
import { users } from 'utils/tests/mocks/models/users';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';
import { S3DatasetBlob } from 'core/shared/utils/tests/mocks/Versioning/blobMocks';
import { branchesAndTagsListHelpers } from 'core/shared/view/domain/Versioning/RepositoryData/BranchesAndTagsList/__tests__/helpers';

import RepositoryData from '../RepositoryData';

const currentWorkspace = userWorkspacesWithCurrentUser.user;
const repository: IRepository = repositories[0];
const commit: IHydratedCommit = {
  author: users[0],
  dateCreated: new Date(),
  message: 'message',
  sha: 'commit-sha',
  parentShas: ['adfadf'],
  type: 'withParent',
};

const repositoryDataParams: GetRouteParams<typeof routes.repositoryData> = {
  repositoryName: repository.name,
  workspaceName: currentWorkspace.name,
};

jest.mock('core/services/versioning/repositoryData/RepositoryDataService');
const mockedRepositoryDataService = makeMockedService({
  path: 'core/services/versioning/repositoryData/RepositoryDataService',
  service: RepositoryDataService,
});

const makeComponent = async ({
  tags,
  pathname,
  commitWithData,
}: {
  pathname: string;
  tags: CommitTag[] | null;
  commitWithData: ICommitWithData;
}) => {
  if (tags) {
    mockedRepositoryDataService.mockMethod(
      'loadTags',
      jest.fn(async () => tags)
    );
  }
  mockedRepositoryDataService.mockMethod(
    'loadBranches',
    jest.fn(async () => ['branch1', defaultBranch])
  );
  const loadCommitWithDataSpy = mockedRepositoryDataService.mockMethod(
    'loadCommitWithData',
    jest.fn(async () => commitWithData)
  );

  const data = await makeMountComponentWithPredefinedData({
    settings: {
      pathname,
    },
    Component: () => (
      <Switch>
        <Route
          path={[
            routes.repositoryData.getPath(),
            routes.repositoryDataWithLocation.getPath(),
          ]}
          component={() => (
            <RepositoryData
              onShowNotFoundError={jest.fn}
              repository={repository}
            />
          )}
        />
      </Switch>
    ),
  });
  return {
    ...data,
    loadCommitWithDataSpy,
  };
};

const findDataLinkHref = (name: string, component: ReactWrapper) =>
  findDataLink(name, component).prop('href');
const findDataLink = (name: string, component: ReactWrapper) => {
  return findByText(name, component).closest('a');
};

const checkFolderContent = (folder: IFolder, component: ReactWrapper) => {
  const subFolder = folder.subFolders[0];
  expect(findByText(subFolder.name, component).length).toEqual(1);

  const blob = folder.blobs[0];
  if (blob) {
    expect(findByText(blob.name, component).length).toEqual(1);
  }
};
const checkFolderContentWithLinks = (
  folder: IFolder,
  component: ReactWrapper
) => {
  const subFolder = folder.subFolders[0];
  const subFolderExpectedLink = `/${currentWorkspace.name}/repositories/${
    repository.name
  }/data/folder/master/${subFolder.name}`;
  expect(findByText(subFolder.name, component).length).toEqual(1);
  expect(findDataLinkHref(subFolder.name, component)).toEqual(
    subFolderExpectedLink
  );

  const blob = folder.blobs[0];
  expect(findByText(blob.name, component).length).toEqual(1);
  expect(findDataLinkHref(blob.name, component)).toEqual(
    `/${currentWorkspace.name}/repositories/${
      repository.name
    }/data/blob/master/${blob.name}`
  );
};

describe('(feature repositoryData)', () => {
  describe('(view) RepositoryData', () => {
    describe('when tags and branches are loaded', () => {
      it('should load a commit by a commit pointer and data by a specific location from URL and display a commit pointer in the dropdown on mount', async () => {
        const root = B.root([
          B.folder('client', { createdByCommitSha: commit.sha }, [
            B.folder('client-2', { createdByCommitSha: commit.sha }, []),
            B.blob(
              '.gitignore',
              { createdByCommitSha: commit.sha },
              S3DatasetBlob
            ),
          ]),
        ]);

        const tags = ['tag1', 'tag2'];
        const currentTag = tags[0];
        const clientFullDataLocationComponents: IFullDataLocationComponents = {
          location: root.elements.client.location,
          commitPointer: CommitPointerHelpers.makeFromTag(currentTag),
          type: 'folder',
        };

        const { component, loadCommitWithDataSpy } = await makeComponent({
          tags,
          commitWithData: { commit, data: root.elements.client.asDataElement },
          pathname: `${routes.repositoryData.getRedirectPath(
            repositoryDataParams
          )}/folder/${currentTag}/${DataLocation.toPathname(
            clientFullDataLocationComponents.location
          )}`,
        });

        const dataRequest: IDataRequest = {
          repositoryId: repository.id,
          fullDataLocationComponents: clientFullDataLocationComponents,
        };
        expect(loadCommitWithDataSpy).toBeCalledWith(dataRequest);
        expect(component.find(Preloader).length).toEqual(1);

        await flushAllPromisesFor(component);

        checkFolderContent(root.elements.client.asDataElement, component);
        expect(branchesAndTagsListHelpers.findSelectedValue(component)).toEqual(
          currentTag
        );
      });

      describe('when commit with data loaded', () => {
        describe('when user in a folder', () => {
          it('should display subfolders and blobs as links', async () => {
            const root = B.root([
              B.folder('client', { createdByCommitSha: commit.sha }, [
                B.folder('subfolder-2', { createdByCommitSha: commit.sha }, []),
              ]),
              B.blob(
                '.gitignore',
                { createdByCommitSha: commit.sha },
                S3DatasetBlob
              ),
            ]);
            const commitWithData: ICommitWithData = {
              commit,
              data: root.asDataElement,
            };
            const { component } = await makeComponent({
              tags: [],
              commitWithData,
              pathname: routes.repositoryData.getRedirectPath(
                repositoryDataParams
              ),
            });

            await flushAllPromisesFor(component);

            checkFolderContentWithLinks(root.asDataElement, component);
          });

          it('should redirect to chosen subfolder when a user click on it', async () => {
            const root = B.root([
              B.folder('client', { createdByCommitSha: commit.sha }, [
                B.folder('subfolder-2', { createdByCommitSha: commit.sha }, []),
              ]),
              B.blob(
                '.gitignore',
                { createdByCommitSha: commit.sha },
                S3DatasetBlob
              ),
            ]);

            const { component, history } = await makeComponent({
              tags: [],
              commitWithData: { commit, data: root.asDataElement },
              pathname: routes.repositoryData.getRedirectPath(
                repositoryDataParams
              ),
            });
            await flushAllPromisesFor(component);

            const spy = mockedRepositoryDataService.mockMethod(
              'loadCommitWithData',
              jest.fn(async () => ({
                commit,
                data: root.elements.client.asDataElement,
              }))
            );
            const loadCommitWithDataSettings: IDataRequest = {
              repositoryId: repository.id,
              fullDataLocationComponents: {
                type: 'folder',
                commitPointer: defaultCommitPointer,
                location: root.elements.client.location,
              },
            };

            findDataLink(root.elements.client.name, component).simulate(
              'click',
              {
                button: 0,
              }
            );

            // while we have loading we should update url and display old content with prealoder
            expect(history.location.pathname).toEqual(
              `/${currentWorkspace.name}/repositories/${
                repository.name
              }/data/folder/master/${root.elements.client.name}`
            );
            expect(spy).toBeCalledWith(loadCommitWithDataSettings);
            checkFolderContent(root.asDataElement, component);
            expect(component.find(Preloader).length).toEqual(1);

            await flushAllPromisesFor(component);
            checkFolderContent(root.elements.client.asDataElement, component);
          });
        });
      });
    });
  });
});
