import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import {
  ComparedCommitType,
  elementDiffMakers,
} from 'core/shared/models/Versioning/Blob/Diff';
import { findByDataTestAttribute } from 'core/shared/utils/tests/react/helpers';

import GitDiffView from '../GitDiffView';
import { diffColors } from '../../../shared/styles';

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
  const commibColumn = component
    .find('tbody')
    .find('tr')
    .first()
    .find('td')
    .at(type === 'A' ? 1 : 2);
  const gitHash = findByDataTestAttribute('git-hash', commibColumn);
  return {
    hash:
      gitHash.length > 0
        ? {
            value: gitHash.text(),
            diffColor:
              gitHash.prop('data-root-styles').backgroundColor ===
              diffColors.green
                ? 'green'
                : gitHash.prop('data-root-styles').backgroundColor ===
                  diffColors.red
                ? 'red'
                : undefined,
          }
        : { value: undefined, diffColor: undefined },
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
      hash: { value: undefined, diffColor: undefined },
    });
    expect(getDisplayedProperties('B', component)).toMatchObject({
      hash: { value: '#', diffColor: 'green' },
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
      hash: { value: '#', diffColor: 'red' },
    });
    expect(getDisplayedProperties('B', component)).toMatchObject({
      hash: { value: undefined, diffColor: undefined },
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
      hash: { value: '#', diffColor: 'red' },
    });
    expect(getDisplayedProperties('B', component)).toMatchObject({
      hash: { value: '######', diffColor: 'green' },
    });
  });
});
