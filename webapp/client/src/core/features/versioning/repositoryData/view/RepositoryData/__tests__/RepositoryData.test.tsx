import * as React from 'react';
import { Switch, Route } from 'react-router';
import routes, { GetRouteParams } from 'core/shared/routes';

import * as B from 'core/shared/models/Versioning/BuildCommitTree';
import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import {
  CommitTag,
  CommitPointerHelpers,
  defaultCommitPointer,
  CommitPointer,
  IHydratedCommit,
  IFolder,
} from 'core/shared/models/Versioning/RepositoryData';
import wait from 'core/shared/utils/tests/integrations/wait';
import waitFor from 'core/shared/utils/tests/integrations/waitFor';
import { S3DatasetBlob } from 'core/shared/utils/tests/mocks/models/Versioning/blobMocks';
import { commits } from 'core/shared/utils/tests/mocks/models/Versioning/commitsMocks';
import { repositories } from 'core/shared/utils/tests/mocks/models/Versioning/repositoriesMocks';
import {
  withAct,
  findByDataTestAttribute,
} from 'core/shared/utils/tests/react/helpers';
import { createBranchesAndTagsListHelpers } from 'core/shared/view/domain/Versioning/RepositoryData/BranchesAndTagsList/__tests__/helpers';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import makeMountComponentWithPredefinedData from 'core/shared/utils/tests/integrations/makeMountComponentWithPredefinedData';
import { userWorkspacesWithCurrentUser } from 'core/shared/utils/tests/mocks/models/workspace';

import RepositoryData from '../RepositoryData';
import {
  checkFolderContentWithLinks,
  viewCommitComponent,
  checkFolderContent,
} from './helpers';
import {
  makeBranchesAndTagsQuery,
  makeCommitWithComponentQuery,
} from './queries';
import { createWaitForPred } from '../../../../../../shared/utils/tests/integrations/waitForByPred';

const makeComponent = async ({
  tags,
  commitPointer,
  folder,
  location,
  commit,
  commitComponents,
}: {
  tags: CommitTag[];
  commitPointer: CommitPointer;
  location: CommitComponentLocation.CommitComponentLocation;
  commit: IHydratedCommit;
  folder: IFolder;
  commitComponents?: [
    {
      location: CommitComponentLocation.CommitComponentLocation;
      folder: IFolder;
    }
  ];
}) => {
  const branchesAndTagsQuery = makeBranchesAndTagsQuery(repository.id, {
    tags,
    branches: [defaultCommitPointer.value],
  });

  return withAct(async () => {
    const data = await makeMountComponentWithPredefinedData({
      settings: {
        pathname: routes.repositoryDataWithLocation.getRedirectPath({
          ...repositoryDataParams,
          commitPointer: commitPointer,
          location: location,
          type: 'folder',
        }),
      },
      apolloMockedProviderProps: {
        mocks: [
          branchesAndTagsQuery,
          makeCommitWithComponentQuery(repository.id, {
            commitPointer: commitPointer,
            location: location,
            commitWithComponent: {
              commit: commit,
              component: folder,
            },
          }),
        ].concat(
          commitComponents
            ? commitComponents.map(c =>
                makeCommitWithComponentQuery(repository.id, {
                  commitPointer: commitPointer,
                  location: c.location,
                  commitWithComponent: {
                    commit: commit,
                    component: c.folder,
                  },
                })
              )
            : []
        ),
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
    return data;
  });
};

const currentWorkspace = userWorkspacesWithCurrentUser.user;
const repository = repositories[0];
const commit = commits[0];
const repositoryDataParams: GetRouteParams<typeof routes.repositoryData> = {
  repositoryName: repository.name,
  workspaceName: currentWorkspace.name,
};

const mockRoot = B.root([
  B.folder('client', [
    B.folder('client-2', []),
    B.blob('.gitignore', S3DatasetBlob),
  ]),
  B.blob('.gitignore', S3DatasetBlob),
]);

const waitForContentDisplaying = createWaitForPred(
  component =>
    findByDataTestAttribute('repository-data-content', component).length === 1
);

const branchesAndTagsListHelpers = createBranchesAndTagsListHelpers(
  'branches-and-tags'
);

describe('(feature repositoryData)', () => {
  describe('(view) RepositoryData', () => {
    describe('when tags and branches are loaded', () => {
      it('should load a commit by a commit pointer and data by a specific location from URL and display a commit pointer in the dropdown on mount', async () => {
        const tags = ['tag1', 'tag2'];
        const commitPointer = CommitPointerHelpers.makeFromTag(tags[0]);
        const clientFolder = mockRoot.elements.client;

        const { component } = await makeComponent({
          tags,
          commit: commit,
          commitPointer: commitPointer,
          folder: clientFolder.asCommitElement,
          location: clientFolder.location,
        });

        expect(component.find(Preloader).length).toEqual(1);

        await waitFor(component);

        checkFolderContent(mockRoot.elements.client.asCommitElement, component);
        expect(branchesAndTagsListHelpers.findSelectedValue(component)).toEqual(
          commitPointer.value
        );
      });

      describe('when commit with data loaded', () => {
        describe('when loading new commit', () => {
          it('should display data content with preloader', async () => {
            const root = mockRoot;
            const nextSubfolder = mockRoot.elements.client;

            const { component } = await makeComponent({
              tags: [],
              commit: commit,
              commitPointer: defaultCommitPointer,
              location: [],
              folder: root.asCommitElement,
              commitComponents: [
                {
                  location: nextSubfolder.location,
                  folder: nextSubfolder.asCommitElement,
                },
              ],
            });

            await waitForContentDisplaying(component);
            expect(
              findByDataTestAttribute('repository-data-content', component)
                .length
            ).toEqual(1);
            expect(component.find(Preloader).length).toEqual(0);

            viewCommitComponent(root.elements.client.name, component);
            await wait();

            expect(
              findByDataTestAttribute('repository-data-content', component)
                .length
            ).toEqual(1);
            expect(component.find(Preloader).length).toEqual(1);
          });
        });

        describe('when user in a folder', () => {
          it('should display subfolders and blobs as links', async () => {
            const commitPointer = defaultCommitPointer;
            const root = mockRoot;

            const { component } = await makeComponent({
              tags: [],
              commit: commit,
              commitPointer: commitPointer,
              folder: root.asCommitElement,
              location: [],
            });

            await waitForContentDisplaying(component);

            checkFolderContentWithLinks({
              component,
              currentWorkspace,
              repository,
              folder: root.asCommitElement,
            });
          });

          it('should redirect to chosen subfolder when a user click on it', async () => {
            const root = mockRoot;
            const nextSubfolder = mockRoot.elements.client;

            const { component, history } = await makeComponent({
              tags: [],
              commit: commit,
              commitPointer: defaultCommitPointer,
              location: [],
              folder: root.asCommitElement,
              commitComponents: [
                {
                  location: nextSubfolder.location,
                  folder: nextSubfolder.asCommitElement,
                },
              ],
            });

            await waitForContentDisplaying(component);

            viewCommitComponent(root.elements.client.name, component);
            await wait();

            // while we have loading we should update url and display old content with prealoder
            expect(history.location.pathname).toEqual(
              routes.repositoryDataWithLocation.getRedirectPath({
                ...repositoryDataParams,
                commitPointer: defaultCommitPointer,
                location: nextSubfolder.location,
                type: nextSubfolder.type,
              })
            );
            checkFolderContent(root.asCommitElement, component);
            expect(component.find(Preloader).length).toEqual(1);

            await waitFor(component);
            checkFolderContent(root.elements.client.asCommitElement, component);
          });
        });
      });
    });
  });
});
