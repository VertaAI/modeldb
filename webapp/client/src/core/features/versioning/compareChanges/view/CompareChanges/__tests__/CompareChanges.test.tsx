import React from 'react';
import { Switch, Route } from 'react-router';

import { CompareCommits } from 'core/features/versioning/compareCommits';
import RepositoryDataService from 'core/services/versioning/repositoryData/RepositoryDataService';
import * as B from 'core/shared/models/Versioning/BuildCommitTree';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import {
  CommitPointer,
  ICommitWithComponent,
  Branch,
  CommitTag,
  IHydratedCommit,
} from 'core/shared/models/Versioning/RepositoryData';
import { flushAllPromisesFor } from 'core/shared/utils/tests/integrations/flushAllPromisesFor';
import { makeMockedService } from 'core/shared/utils/tests/integrations/mockServiceMethod';
import { repositories } from 'core/shared/utils/tests/mocks/Versioning/repositoriesMocks';
import { ArgumentTypes } from 'core/shared/utils/types';
import { createBranchesAndTagsListHelpers } from 'core/shared/view/domain/Versioning/RepositoryData/BranchesAndTagsList/__tests__/helpers';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import routes from 'routes';
import makeMountComponentWithPredefinedData from 'utils/tests/integrations/makeMountComponentWithPredefinedData';
import { users } from 'utils/tests/mocks/models/users';
import { userWorkspacesWithCurrentUser } from 'utils/tests/mocks/models/workspace';

import CompareChanges from '../CompareChanges';

const currentWorkspace = userWorkspacesWithCurrentUser.user;
const repository: IRepository = repositories[0];

const commitA: IHydratedCommit = {
  author: users[0],
  dateCreated: new Date(),
  message: 'message',
  sha: 'commit-sha',
  parentShas: ['adfadf'],
  type: 'withParent',
};

const commitB: IHydratedCommit = {
  author: users[0],
  dateCreated: new Date(),
  message: 'messageaa',
  sha: 'commit-shaaaa',
  parentShas: ['commit-sha'],
  type: 'withParent',
};

const commitC: IHydratedCommit = {
  author: users[0],
  dateCreated: new Date(),
  message: 'fafaf',
  sha: 'commit-ccccc',
  parentShas: ['commit-shaaaa'],
  type: 'withParent',
};

const commitPointerA: CommitPointer = {
  type: 'branch',
  value: 'branchA',
};

const commitPointerB: CommitPointer = {
  type: 'branch',
  value: 'branchB',
};

const commitPointerC: CommitPointer = {
  type: 'tag',
  value: 'tagC',
};

const commitsByCommitPointer: Record<
  CommitPointer['value'],
  IHydratedCommit
> = {
  [commitPointerA.value]: commitA,
  [commitPointerB.value]: commitB,
  [commitPointerC.value]: commitC,
};

jest.mock('core/services/versioning/repositoryData/RepositoryDataService');
const mockedRepositoryDataService = makeMockedService({
  path: 'core/services/versioning/repositoryData/RepositoryDataService',
  service: RepositoryDataService,
});

const makeComponent = async ({
  commitPointerValueA,
  commitPointerValueB,
  branches,
  tags,
}: {
  commitPointerValueA: CommitPointer['value'];
  commitPointerValueB: CommitPointer['value'];
  branches: Branch[];
  tags: CommitTag[];
}) => {
  const loadCommitByPointerSpy = mockedRepositoryDataService.mockMethod(
    'loadCommitByPointer',
    jest.fn(async (_, commitPointer: CommitPointer) => {
      return Promise.resolve(commitsByCommitPointer[commitPointer.value]);
    })
  );

  const data = await makeMountComponentWithPredefinedData({
    settings: {
      pathname: routes.repositoryCompareChanges.getRedirectPath({
        workspaceName: currentWorkspace.name,
        commitPointerAValue: commitPointerValueA,
        commitPointerBValue: commitPointerValueB,
        repositoryName: repository.name,
      }),
    },
    Component: () => (
      <Switch>
        <Route
          path={routes.repositoryCompareChanges.getPath()}
          component={() => (
            <CompareChanges
              repository={repository}
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

  return {
    ...data,
    loadCommitByPointerSpy,
  };
};

describe('(feature copmareChanges)', () => {
  describe('(view CompareChanges)', () => {
    it('should load commits by commit pointers and display compare between commits', async () => {
      const { component, loadCommitByPointerSpy } = await makeComponent({
        branches: [commitPointerA.value, commitPointerB.value],
        tags: [commitPointerC.value],
        commitPointerValueA: commitPointerA.value,
        commitPointerValueB: commitPointerB.value,
      });

      const repositoryDataService = new RepositoryDataService();
      const requestDataA: ArgumentTypes<
        typeof repositoryDataService.loadCommitByPointer
      > = [repository.id, commitPointerA];
      const requestDataB: ArgumentTypes<
        typeof repositoryDataService.loadCommitByPointer
      > = [repository.id, commitPointerB];

      expect(loadCommitByPointerSpy).toHaveBeenNthCalledWith(
        1,
        ...requestDataA
      );
      expect(loadCommitByPointerSpy).toHaveBeenNthCalledWith(
        2,
        ...requestDataB
      );
      expect(component.find(Preloader).length).toEqual(1);

      await flushAllPromisesFor(component);

      const expectedCompareCommitsProps: React.ComponentProps<
        typeof CompareCommits
      > = {
        commitASha: commitA.sha,
        commitBSha: commitB.sha,
        repository,
      };

      expect(component.find(CompareCommits).props()).toEqual(
        expectedCompareCommitsProps
      );
    });

    it('should change commit pointer and load new commit by new commit pointer', async () => {
      const { component, loadCommitByPointerSpy } = await makeComponent({
        branches: [commitPointerA.value, commitPointerB.value],
        tags: [commitPointerC.value],
        commitPointerValueA: commitPointerA.value,
        commitPointerValueB: commitPointerB.value,
      });

      await flushAllPromisesFor(component);

      const branchesAndTagsListHelpers = createBranchesAndTagsListHelpers(
        'commit-pointer-b'
      );

      branchesAndTagsListHelpers.openTab('tags', component);

      branchesAndTagsListHelpers.changeCommitPointer(
        commitPointerC.value,
        component
      );

      expect(window.location.pathname).toEqual(
        routes.repositoryCompareChanges.getRedirectPath({
          commitPointerAValue: commitPointerA.value,
          commitPointerBValue: commitPointerC.value,
          workspaceName: currentWorkspace.name,
          repositoryName: repository.name,
        })
      );
    });
  });
});
