import * as React from 'react';
import { Switch, Route } from 'react-router';
import routes, { GetRouteParams } from 'routes';

import RepositoryDataService from 'core/services/versioning/repositoryData/RepositoryDataService';
import * as B from 'core/shared/models/Versioning/BuildCommitTree';
import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  CommitTag,
  IHydratedCommit,
  ICommitComponentRequest,
  IFullCommitComponentLocationComponents,
  CommitPointerHelpers,
  defaultCommitPointer,
  defaultBranch,
  ICommitWithComponent,
} from 'core/shared/models/Versioning/RepositoryData';
import { flushAllPromisesFor } from 'core/shared/utils/tests/integrations/flushAllPromisesFor';
import { makeMockedService } from 'core/shared/utils/tests/integrations/mockServiceMethod';
import { findByDataTestAttribute } from 'core/shared/utils/tests/react/helpers';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import makeMountComponentWithPredefinedData from 'utils/tests/integrations/makeMountComponentWithPredefinedData';
import { repositories } from 'core/shared/utils/tests/mocks/Versioning/repositoriesMocks';
import { users } from 'utils/tests/mocks/models/users';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';
import { S3DatasetBlob } from 'core/shared/utils/tests/mocks/Versioning/blobMocks';
import { createBranchesAndTagsListHelpers } from 'core/shared/view/domain/Versioning/RepositoryData/BranchesAndTagsList/__tests__/helpers';
import delay from 'core/shared/utils/delay';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';

import RepositoryData from '../RepositoryData';
import {
  checkFolderContentWithLinks,
  viewDataElement,
  checkFolderContent,
} from './helpers';

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

const branchesAndTagsListHelpers = createBranchesAndTagsListHelpers(
  'branches-and-tags'
);

jest.mock('core/services/versioning/repositoryData/RepositoryDataService');
const mockedRepositoryDataService = makeMockedService({
  path: 'core/services/versioning/repositoryData/RepositoryDataService',
  service: RepositoryDataService,
});

const makeComponent = async ({
  tags,
  pathname,
  loadCommitWithComponent,
}: {
  pathname: string;
  tags: CommitTag[];
  loadCommitWithComponent: () => Promise<ICommitWithComponent>;
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
  const loadCommitWithComponentSpy = mockedRepositoryDataService.mockMethod(
    'loadCommitWithComponent',
    jest.fn(loadCommitWithComponent)
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
    loadCommitWithComponentSpy,
  };
};

const mockRoot = B.root([
  B.folder('client', { createdByCommitSha: commit.sha }, [
    B.folder('client-2', { createdByCommitSha: commit.sha }, []),
    B.blob('.gitignore', { createdByCommitSha: commit.sha }, S3DatasetBlob),
  ]),
]);

describe('(feature repositoryData)', () => {
  describe('(view) RepositoryData', () => {
    describe('when tags and branches are loaded', () => {
      it('should load a commit by a commit pointer and data by a specific location from URL and display a commit pointer in the dropdown on mount', async () => {
        const tags = ['tag1', 'tag2'];
        const currentTag = tags[0];
        const clientFullCommitComponentLocationComponents: IFullCommitComponentLocationComponents = {
          location: mockRoot.elements.client.location,
          commitPointer: CommitPointerHelpers.makeFromTag(currentTag),
          type: 'folder',
        };

        const { component, loadCommitWithComponentSpy } = await makeComponent({
          tags,
          loadCommitWithComponent: async () => ({
            commit,
            component: mockRoot.elements.client.asDataElement,
          }),
          pathname: `${routes.repositoryData.getRedirectPath(
            repositoryDataParams
          )}/folder/${currentTag}/${CommitComponentLocation.toPathname(
            clientFullCommitComponentLocationComponents.location
          )}`,
        });

        const dataRequest: ICommitComponentRequest = {
          repositoryId: repository.id,
          fullCommitComponentLocationComponents: clientFullCommitComponentLocationComponents,
        };
        expect(loadCommitWithComponentSpy).toBeCalledWith(dataRequest);
        expect(component.find(Preloader).length).toEqual(1);

        await flushAllPromisesFor(component);

        checkFolderContent(mockRoot.elements.client.asDataElement, component);
        expect(branchesAndTagsListHelpers.findSelectedValue(component)).toEqual(
          currentTag
        );
      });

      describe('loading commit with data', () => {
        const makeComponentForTestSuit = async ({
          loadCommitWithComponent,
        }: {
          loadCommitWithComponent: () => Promise<ICommitWithComponent>;
        }) => {
          const data = await makeComponent({
            loadCommitWithComponent,
            pathname: routes.repositoryData.getRedirectPath(
              repositoryDataParams
            ),
            tags: [],
          });

          await flushAllPromisesFor(data.component);

          return data;
        };

        describe('when there are not commit with data', () => {
          it('should display only preloader when commit with data are loading', async () => {
            const { component } = await makeComponentForTestSuit({
              loadCommitWithComponent: async () => {
                await delay(500);
                return { commit, component: mockRoot.asDataElement };
              },
            });

            expect(component.find(Preloader).length).toEqual(1);
            expect(
              findByDataTestAttribute('repository-data-content', component)
                .length
            ).toEqual(0);
          });

          it('should display only an error when loading commit with data are failure', async () => {
            const { component } = await makeComponentForTestSuit({
              loadCommitWithComponent: async () => {
                throw new Error('error');
              },
            });

            expect(component.find(InlineCommunicationError).length).toEqual(1);
            expect(
              findByDataTestAttribute('repository-data-content', component)
                .length
            ).toEqual(0);
          });

          it('should display loaded commit with data', async () => {
            const { component } = await makeComponentForTestSuit({
              loadCommitWithComponent: async () => {
                return {
                  commit,
                  component: mockRoot.elements.client.asDataElement,
                };
              },
            });

            expect(component.find(Preloader).length).toEqual(0);
            expect(component.find(InlineCommunicationError).length).toEqual(0);
            expect(
              findByDataTestAttribute('repository-data-content', component)
                .length
            ).toEqual(1);
          });
        });

        describe('when there are commit with data and loading new data of blob or folder', () => {
          it('should display data content with preloader', async () => {
            let i = 0;
            const { component } = await makeComponentForTestSuit({
              loadCommitWithComponent: async () => {
                if (i === 0) {
                  i++;
                  return { commit, component: mockRoot.asDataElement };
                } else {
                  await delay(500);
                  return {
                    commit,
                    component: mockRoot.elements.client.asDataElement,
                  };
                }
              },
            });

            expect(
              findByDataTestAttribute('repository-data-content', component)
                .length
            ).toEqual(1);
            expect(component.find(Preloader).length).toEqual(0);

            viewDataElement(mockRoot.elements.client.name, component);

            expect(
              findByDataTestAttribute('repository-data-content', component)
                .length
            ).toEqual(1);
            expect(component.find(Preloader).length).toEqual(1);
          });
        });
      });

      describe('when commit with data loaded', () => {
        describe('when user in a folder', () => {
          it('should display subfolders and blobs as links', async () => {
            const root = mockRoot.elements.client;
            const commitWithComponent: ICommitWithComponent = {
              commit,
              component: root.asDataElement,
            };
            const { component } = await makeComponent({
              tags: [],
              loadCommitWithComponent: async () => commitWithComponent,
              pathname: routes.repositoryData.getRedirectPath(
                repositoryDataParams
              ),
            });

            await flushAllPromisesFor(component);

            checkFolderContentWithLinks({
              component,
              currentWorkspace,
              repository,
              folder: root.asDataElement,
            });
          });

          it('should redirect to chosen subfolder when a user click on it', async () => {
            const root = mockRoot;

            const { component, history } = await makeComponent({
              tags: [],
              loadCommitWithComponent: async () => ({
                commit,
                component: root.asDataElement,
              }),
              pathname: routes.repositoryData.getRedirectPath(
                repositoryDataParams
              ),
            });
            await flushAllPromisesFor(component);

            const spy = mockedRepositoryDataService.mockMethod(
              'loadCommitWithComponent',
              jest.fn(async () => ({
                commit,
                component: root.elements.client.asDataElement,
              }))
            );
            const loadCommitWithComponentSettings: ICommitComponentRequest = {
              repositoryId: repository.id,
              fullCommitComponentLocationComponents: {
                type: 'folder',
                commitPointer: defaultCommitPointer,
                location: root.elements.client.location,
              },
            };

            viewDataElement(root.elements.client.name, component);

            // while we have loading we should update url and display old content with prealoder
            expect(history.location.pathname).toEqual(
              `/${currentWorkspace.name}/repositories/${
                repository.name
              }/data/folder/master/${root.elements.client.name}`
            );
            expect(spy).toBeCalledWith(loadCommitWithComponentSettings);
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
