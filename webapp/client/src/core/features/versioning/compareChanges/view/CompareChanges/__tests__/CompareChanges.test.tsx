import { MockedResponse } from '@apollo/react-testing';
import { ReactWrapper } from 'enzyme';
import React from 'react';
import { Switch, Route } from 'react-router';

import { IServerCodeBlobDiff } from 'core/services/serverModel/Versioning/CompareCommits/convertServerCodeDiff';
import { IServerBlobDiff } from 'core/services/serverModel/Versioning/CompareCommits/ServerDiff';
import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import {
  CommitPointer,
  Branch,
  CommitTag,
  CommitPointerHelpers,
  SHA,
} from 'core/shared/models/Versioning/RepositoryData';
import { flushAllPromisesFor } from 'core/shared/utils/tests/integrations/flushAllPromisesFor';
import waitFor from 'core/shared/utils/tests/integrations/waitFor';
import { commits } from 'core/shared/utils/tests/mocks/Versioning/commitsMocks';
import { repositories } from 'core/shared/utils/tests/mocks/Versioning/repositoriesMocks';
import {
  findByText,
  withAct,
  findByDataTestAttribute,
} from 'core/shared/utils/tests/react/helpers';
import { createBranchesAndTagsListHelpers } from 'core/shared/view/domain/Versioning/RepositoryData/BranchesAndTagsList/__tests__/helpers';
import { shortenSHA } from 'core/shared/view/domain/Versioning/ShortenedSHA/ShortenedSHA';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { AllEntityAction } from 'core/shared/models/EntitiesActions';
import routes from 'routes';
import makeMountComponentWithPredefinedData from 'utils/tests/integrations/makeMountComponentWithPredefinedData';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

import CompareChanges from '../CompareChanges';

import {
  makeMergeCommitsMutationMock,
  makeUpdateBranchMutationMock,
  makeCompareChangesQuery,
  MakeCompareChangesQuerySettings,
} from './queries';
import { createWaitForPred } from 'core/shared/utils/tests/integrations/waitForByPred';
import { createWaitForExpect } from 'core/shared/utils/tests/integrations/waitForExpect';
import { getDisplayedNotifications } from 'core/shared/view/elements/Notification/__tests__/helpers';

const ownerAllowedActions: any = [];

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
  commitPointerA: CommitPointer;
  commitPointerB: CommitPointer;
  branches: Branch[];
  tags: CommitTag[];
  compareChangesQuery: MockedResponse;
  allowedActions: any;

  graphqlMocks?: MockedResponse[];
};

const makeComponent = async ({
  commitPointerA,
  commitPointerB,
  branches,
  tags,
  compareChangesQuery,
  allowedActions,
  graphqlMocks,
}: MakeComponentProps) => {
  const repositoryCompareChangesPathname = routes.repositoryCompareChanges.getRedirectPath(
    {
      workspaceName: currentWorkspace.name,
      commitPointerAValue: commitPointerA.value,
      commitPointerBValue: commitPointerB.value,
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
                repository={{ ...repository }}
                branches={branches}
                tags={tags}
                commitPointerA={commitPointerA}
                commitPointerB={commitPointerB}
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
  diffs: Array<IServerBlobDiff<any>>,
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

  commitPointerA: commitPointersWithCommitsSha.A.pointer,
  commitPointerB: commitPointersWithCommitsSha.B.pointer,

  compareChangesQuery: makeCompareChangesQueryWithDefault([]),
  allowedActions: ownerAllowedActions,
};

describe.only('(feature copmareChanges)', () => {
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
          commitPointerA: opts ? opts.A.pointer : defaultProps.commitPointerA,
          commitPointerB: opts ? opts.B.pointer : defaultProps.commitPointerB,
          graphqlMocks,
          allowedActions: ownerAllowedActions,
          compareChangesQuery: makeCompareChangesQueryWithDefault(
            serverDiffs,
            opts
          ),
        };
      };

      it('should display a notification and redirect to conflicts page when there is a merge conflict', async () => {
        const { component, history } = await makeComponent(
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
        await waitFor(component);

        expect(await getDisplayedNotifications(component)).toEqual([
          {
            type: 'error',
            content: 'Merge not possible!',
          },
        ]);
        expect(history.location.pathname).toEqual(
          routes.repositoryMergeConflicts.getRedirectPath({
            repositoryName: repository.name,
            commitPointerAValue: commitPointersWithCommitsSha.A.pointer.value,
            commitPointerBValue: commitPointersWithCommitsSha.B.pointer.value,
            workspaceName: currentWorkspace.name,
          })
        );
      });

      it('should update base with new commit and redirect to base after successfull merging if base is branch', async () => {
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

        await createWaitForPred(component => {
          return (
            findByDataTestAttribute('compare-changes', component).length === 1
          );
        })(component);
        await mergeCommits(component);

        await createWaitForExpect(() => {
          expect(history.location.pathname).toEqual(
            routes.repositoryDataWithLocation.getRedirectPath({
              repositoryName: repository.name,
              workspaceName: currentWorkspace.name,
              commitPointer: base.pointer,
              location: CommitComponentLocation.makeRoot(),
              type: 'folder',
            })
          );
        })(component);
        expect(await getDisplayedNotifications(component)).toEqual([]);
      });

      it('should redirect to base after successfull merging if base is not branch', async () => {
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

        await createWaitForPred(component => {
          return (
            findByDataTestAttribute('compare-changes', component).length === 1
          );
        })(component);
        await mergeCommits(component);

        await createWaitForExpect(() => {
          expect(history.location.pathname).toEqual(
            routes.repositoryDataWithLocation.getRedirectPath({
              repositoryName: repository.name,
              workspaceName: currentWorkspace.name,
              commitPointer: base.pointer,
              location: CommitComponentLocation.makeRoot(),
              type: 'folder',
            })
          );
        })(component);
        expect(await getDisplayedNotifications(component)).toEqual([]);
      });
    });
  });
});
