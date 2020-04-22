import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import {
  ComparedCommitType,
  elementDiffMakers,
} from 'core/shared/models/Versioning/Blob/Diff';

import GitDiffView from '../GitDiffView';
import { getCommitColumnInfo } from '../../../shared/ComparePropertiesTable/__tests__/helpers';

const comparedCommitsInfo: React.ComponentProps<
  typeof GitDiffView
>['comparedCommitsInfo'] = {
  commitA: {
    sha: 'commitASha',
  },
  commitB: {
    sha: 'commitBSHa',
  },
};

type Diff = React.ComponentProps<typeof GitDiffView>['diff'];

const makeComponent = ({ diff }: { diff: Diff }) => {
  return mount(
    <GitDiffView comparedCommitsInfo={comparedCommitsInfo} diff={diff} />
  );
};

const getDisplayedProperties = (
  type: ComparedCommitType,
  component: ReactWrapper
) => {
  return {
    hash: getCommitColumnInfo(
      type,
      'hash',
      column => column.text() || undefined,
      component
    ),
  };
};

describe('(compareCommits feature) GitDiffView', () => {
  it('should display git properties with green highlithing in the B column when diff status is added', () => {
    const addedDiff: Diff = elementDiffMakers.added({
      type: 'git',
      data: {
        branch: 'branch',
        commitHash: '#',
        isDirty: false,
        remoteRepoUrl: { type: 'unknown', value: 'repo' },
        tag: null,
      },
    });
    const component = makeComponent({ diff: addedDiff });

    expect(getDisplayedProperties('A', component)).toMatchObject({
      hash: { content: undefined, diffColor: undefined },
    });
    expect(getDisplayedProperties('B', component)).toMatchObject({
      hash: { content: '#', diffColor: 'green' },
    });
  });

  it('should display git properties with red highlithing in the A column when diff status is deleted', () => {
    const deletedDiff: Diff = elementDiffMakers.deleted({
      type: 'git',
      data: {
        branch: 'branch',
        commitHash: '#',
        isDirty: false,
        remoteRepoUrl: { type: 'unknown', value: 'repo' },
        tag: null,
      },
    });
    const component = makeComponent({ diff: deletedDiff });

    expect(getDisplayedProperties('A', component)).toMatchObject({
      hash: { content: '#', diffColor: 'red' },
    });
    expect(getDisplayedProperties('B', component)).toMatchObject({
      hash: { content: undefined, diffColor: undefined },
    });
  });

  it('should display different git properties with appropriated highlighting in A/B column and without highlighting when they are not different when diff status is modified', () => {
    const modifiedDiff: Diff = elementDiffMakers.modified(
      {
        data: {
          branch: 'branch',
          commitHash: '#',
          isDirty: false,
          remoteRepoUrl: { type: 'unknown', value: 'repo' },
          tag: null,
        },
        type: 'git',
      },
      {
        type: 'git',
        data: {
          branch: 'branch',
          commitHash: '######',
          isDirty: false,
          remoteRepoUrl: { type: 'unknown', value: 'repo' },
          tag: null,
        },
      }
    );
    const component = makeComponent({ diff: modifiedDiff });

    expect(getDisplayedProperties('A', component)).toMatchObject({
      hash: { content: '#', diffColor: 'red' },
    });
    expect(getDisplayedProperties('B', component)).toMatchObject({
      hash: { content: '######', diffColor: 'green' },
    });
  });
});
