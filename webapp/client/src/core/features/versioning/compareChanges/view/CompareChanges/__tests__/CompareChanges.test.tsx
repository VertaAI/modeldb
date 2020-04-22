import React from 'react';
import { Switch, Route } from 'react-router';
import { MockedResponse } from '@apollo/react-testing';

import {
  CommitPointer,
  Branch,
  CommitTag,
  IHydratedCommit,
  CommitPointerHelpers,
  SHA,
} from 'core/shared/models/Versioning/RepositoryData';
import { flushAllPromisesFor } from 'core/shared/utils/tests/integrations/flushAllPromisesFor';
import { repositories } from 'core/shared/utils/tests/mocks/Versioning/repositoriesMocks';
import { createBranchesAndTagsListHelpers } from 'core/shared/view/domain/Versioning/RepositoryData/BranchesAndTagsList/__tests__/helpers';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import routes from 'routes';
import makeMountComponentWithPredefinedData from 'utils/tests/integrations/makeMountComponentWithPredefinedData';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';
import {
  findByText,
  withAct,
  findByDataTestAttribute,
} from 'core/shared/utils/tests/react/helpers';
import { shortenSHA } from 'core/shared/view/domain/Versioning/ShortenedSHA/ShortenedSHA';
import { commits } from 'core/shared/utils/tests/mocks/Versioning/commitsMocks';
import { ownerAllowedActions } from 'utils/tests/mocks/models/collaborators';
import waitFor from 'core/shared/utils/tests/integrations/waitFor';
import { IServerCodeBlobDiff } from 'core/services/serverModel/Versioning/CompareCommits/convertServerCodeDiff';
import { IServerBlobDiff } from 'core/services/serverModel/Versioning/CompareCommits/ServerDiff';
import { AllEntityAction } from 'models/EntitiesActions';
import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';

import CompareChanges from '../CompareChanges';

import { toast } from 'react-toastify';
import {
  makeMergeCommitsMutationMock,
  makeUpdateBranchMutationMock,
  makeCompareChangesQuery,
  MakeCompareChangesQuerySettings,
} from './queries';
import { ReactWrapper } from 'enzyme';
jest.mock('react-toastify');
const mockedToast = (toast as any) as jest.Mock<typeof toast>;

const currentWorkspace = userWorkspacesWithCurrentUser.user;
const repository = repositories[0];

export interface ICommitPointerWithCommitSha {
  pointer: CommitPointer;
  commitSha: SHA;
}

const commitPointersWithCommitsSha = {
  A: {
    pointer: CommitPointerHelpers.makeFromBranch('branchA'),
    commitSha: commits[0].sha,
  },
  B: {
    pointer: CommitPointerHelpers.makeFromBranch('branchB'),
    commitSha: commits[1].sha,
  },
  C: {
    pointer: CommitPointerHelpers.makeFromTag('tagC'),
    commitSha: commits[2].sha,
  },
};

const serverDiffs: IServerCodeBlobDiff[] = [
  {
    location: ['repo_git'],
    status: 'ADDED',
    code: {
      git: {
        status: 'ADDED',
        B: {
          repo: 'repo',
          hash: '#hhh',
          branch: 'master',
          tag: 'tag',
          is_dirty: true,
        },
      },
    },
  },
];

type MakeComponentProps = {
  commitPointerValueA: CommitPointer['value'];
  commitPointerValueB: CommitPointer['value'];
  branches: Branch[];
  tags: CommitTag[];
  compareChangesQuery: MockedResponse;
  allowedActions: AllEntityAction[];

  graphqlMocks?: MockedResponse[];
};

const makeComponent = async ({
  commitPointerValueA,
  commitPointerValueB,
  branches,
  tags,
  compareChangesQuery,
  allowedActions,
  graphqlMocks,
}: MakeComponentProps) => {
  const repositoryCompareChangesPathname = routes.repositoryCompareChanges.getRedirectPath(
    {
      workspaceName: currentWorkspace.name,
      commitPointerAValue: commitPointerValueA,
      commitPointerBValue: commitPointerValueB,
      repositoryName: repository.name,
    }
  );

  const data = await withAct(async () => {
    return await makeMountComponentWithPredefinedData({
      settings: {
        pathname: repositoryCompareChangesPathname,
      },
      apolloMockedProviderProps: {
        mocks: [compareChangesQuery].concat(graphqlMocks || []),
      },
      Component: () => (
        <Switch>
          <Route
            path={routes.repositoryCompareChanges.getPath()}
            component={() => (
              <CompareChanges
                repository={{ ...repository, allowedActions }}
                branches={branches}
                tags={tags}
                commitPointerValueA={commitPointerValueA}
                commitPointerValueB={commitPointerValueB}
              />
            )}
          />
        </Switch>
      ),
    });
  });

  return { ...data, pathname: repositoryCompareChangesPathname };
};

const makeCompareChangesQueryWithDefault = (
  diffs: IServerBlobDiff<any>[],
  opts: Omit<MakeCompareChangesQuerySettings, 'repositoryId'> = {
    A: commitPointersWithCommitsSha.A,
    B: commitPointersWithCommitsSha.B,
  }
) =>
  makeCompareChangesQuery(
    {
      ...opts,
      repositoryId: repository.id,
    },
    diffs
  );

const defaultProps: MakeComponentProps = {
  branches: [
    commitPointersWithCommitsSha.A.pointer.value,
    commitPointersWithCommitsSha.B.pointer.value,
  ],
  tags: [commitPointersWithCommitsSha.C.pointer.value],

  commitPointerValueA: commitPointersWithCommitsSha.A.pointer.value,
  commitPointerValueB: commitPointersWithCommitsSha.B.pointer.value,

  compareChangesQuery: makeCompareChangesQueryWithDefault([]),
  allowedActions: ownerAllowedActions,
};

describe('(feature copmareChanges)', () => {
  describe('(view CompareChanges)', () => {
    it('should load commits by commit pointers and display compare between commits', async () => {
      const { component } = await makeComponent(defaultProps);

      expect(component.find(Preloader).length).toEqual(1);

      await flushAllPromisesFor(component);

      expect(
        findByText(
          shortenSHA(commitPointersWithCommitsSha.A.commitSha),
          component
        ).length
      ).toEqual(1);
      expect(
        findByText(
          shortenSHA(commitPointersWithCommitsSha.B.commitSha),
          component
        ).length
      ).toEqual(1);
      expect(findByText('Nothing to compare', component).length).toEqual(1);
    });

    it('should change commit pointer and load new commit by new commit pointer', async () => {
      const { component } = await makeComponent(defaultProps);

      await flushAllPromisesFor(component);

      const branchesAndTagsListHelpers = createBranchesAndTagsListHelpers(
        'commit-pointer-b'
      );
      branchesAndTagsListHelpers.openTab('tags', component);
      branchesAndTagsListHelpers.changeCommitPointer(
        commitPointersWithCommitsSha.C.pointer.value,
        component
      );

      expect(window.location.pathname).toEqual(
        routes.repositoryCompareChanges.getRedirectPath({
          commitPointerAValue: commitPointersWithCommitsSha.A.pointer.value,
          commitPointerBValue: commitPointersWithCommitsSha.C.pointer.value,
          workspaceName: currentWorkspace.name,
          repositoryName: repository.name,
        })
      );
    });

    describe('merging', () => {
      const findMergeCommitsButton = findByDataTestAttribute(
        'merge-commits-button'
      );

      describe('displaying merging button', () => {
        const checkMergingButtonDisplaying = async (
          {
            allowedActions,
            compareChangesQuery,
          }: Pick<MakeComponentProps, 'allowedActions' | 'compareChangesQuery'>,
          props: { shouldDisplay: boolean }
        ) => {
          const { component } = await makeComponent({
            ...defaultProps,
            compareChangesQuery,
            allowedActions,
          });

          await waitFor(component);

          expect(findMergeCommitsButton(component).length).toEqual(
            props.shouldDisplay ? 1 : 0
          );
        };

        it('should be enable when a user has access and there are diffs', async () => {
          await checkMergingButtonDisplaying(
            {
              compareChangesQuery: makeCompareChangesQueryWithDefault(
                serverDiffs
              ),
              allowedActions: ['update'],
            },
            { shouldDisplay: true }
          );
          await checkMergingButtonDisplaying(
            {
              compareChangesQuery: makeCompareChangesQueryWithDefault([]),
              allowedActions: ['update'],
            },
            { shouldDisplay: false }
          );
          await checkMergingButtonDisplaying(
            {
              compareChangesQuery: makeCompareChangesQueryWithDefault(
                serverDiffs
              ),
              allowedActions: [],
            },
            { shouldDisplay: false }
          );
        });
      });

      const mergeCommits = async (component: ReactWrapper<any>) => {
        await waitFor(component);

        findMergeCommitsButton(component).simulate('click');
        await waitFor(component);
      };
      const makePropsWithEnabledMerging = (
        graphqlMocks: Required<MakeComponentProps>['graphqlMocks'],
        opts?: {
          A: ICommitPointerWithCommitSha;
          B: ICommitPointerWithCommitSha;
        }
      ): MakeComponentProps => {
        return {
          ...defaultProps,
          commitPointerValueA: opts
            ? opts.A.pointer.value
            : defaultProps.commitPointerValueA,
          commitPointerValueB: opts
            ? opts.B.pointer.value
            : defaultProps.commitPointerValueB,
          graphqlMocks,
          allowedActions: ownerAllowedActions,
          compareChangesQuery: makeCompareChangesQueryWithDefault(
            serverDiffs,
            opts
          ),
        };
      };

      it('should display a notification when there are a merge conflict', async () => {
        mockedToast.mockClear();

        const { component, history, pathname } = await makeComponent(
          makePropsWithEnabledMerging([
            makeMergeCommitsMutationMock(
              {
                repositoryId: repository.id,
                commitASha: commitPointersWithCommitsSha.A.commitSha,
                commitBSha: commitPointersWithCommitsSha.B.commitSha,
              },
              { type: 'conflict' }
            ),
          ])
        );

        await mergeCommits(component);

        expect(mockedToast).toBeCalled();
        expect(mockedToast.mock.calls[0][0]).toEqual(
          <span>Merge not possible!</span>
        );
        expect(history.location.pathname).toEqual(pathname);
      });

      it('should update base with new commit and redirect to base after successfull merging if base is branch', async () => {
        mockedToast.mockClear();

        const base = commitPointersWithCommitsSha.A;
        const newCommitSha = 'newCommitSha';
        const { component, history } = await makeComponent(
          makePropsWithEnabledMerging([
            makeMergeCommitsMutationMock(
              {
                repositoryId: repository.id,
                commitASha: base.commitSha,
                commitBSha: commitPointersWithCommitsSha.B.commitSha,
              },
              { type: 'success', newCommitSha }
            ),
            makeUpdateBranchMutationMock({
              baseBranch: base.pointer.value,
              newCommitSha,
              repositoryId: repository.id,
            }),
          ])
        );

        await mergeCommits(component);

        expect(mockedToast).not.toBeCalled();
        expect(history.location.pathname).toEqual(
          routes.repositoryDataWithLocation.getRedirectPath({
            repositoryName: repository.name,
            workspaceName: currentWorkspace.name,
            commitPointer: base.pointer,
            location: CommitComponentLocation.makeRoot(),
            type: 'folder',
          })
        );
      });

      it('should redirect to base after successfull merging if base is not branch', async () => {
        mockedToast.mockClear();

        const base = commitPointersWithCommitsSha.C;
        const B = commitPointersWithCommitsSha.B;
        const newCommitSha = 'newCommitSha';
        const { component, history } = await makeComponent(
          makePropsWithEnabledMerging(
            [
              makeMergeCommitsMutationMock(
                {
                  repositoryId: repository.id,
                  commitASha: base.commitSha,
                  commitBSha: B.commitSha,
                },
                { type: 'success', newCommitSha }
              ),
            ],
            { A: base, B }
          )
        );

        await mergeCommits(component);

        expect(mockedToast).not.toBeCalled();
        expect(history.location.pathname).toEqual(
          routes.repositoryDataWithLocation.getRedirectPath({
            repositoryName: repository.name,
            workspaceName: currentWorkspace.name,
            commitPointer: base.pointer,
            location: CommitComponentLocation.makeRoot(),
            type: 'folder',
          })
        );
      });
    });
  });
});
