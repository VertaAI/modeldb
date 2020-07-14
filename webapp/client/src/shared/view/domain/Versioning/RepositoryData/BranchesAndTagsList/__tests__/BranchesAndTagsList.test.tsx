import * as React from 'react';
import { mount } from 'enzyme';

import { CommitPointerHelpers } from 'shared/models/Versioning/RepositoryData';

import BranchesAndTagsList from '../BranchesAndTagsList';
import { createBranchesAndTagsListHelpers } from './helpers';

type ComponentProps = React.ComponentProps<typeof BranchesAndTagsList>;

const makeComponent = (props: ComponentProps) => {
  return mount(<BranchesAndTagsList dataTest="branches-and-tags" {...props} />);
};

const branchesAndTagsListHelpers = createBranchesAndTagsListHelpers(
  'branches-and-tags'
);

const branches = ['branch1', 'branch2'];
const tags = ['tag1', 'tag2'];

const defaultProps: ComponentProps = {
  branches,
  tags,
  commitPointer: CommitPointerHelpers.makeFromBranch(branches[0]),
  onChangeCommitPointer: jest.fn(),
};

describe('(Versioning components) BranchesAndTagsList', () => {
  describe('displaying branches or tags according to active tab', () => {
    it('should display branches when the "branches" tab is active', () => {
      const component = makeComponent(defaultProps);

      expect(branchesAndTagsListHelpers.getDisplayedItems(component)).toEqual(
        branches
      );
    });

    it('should display branches when the "tags" tab is active', () => {
      const component = makeComponent(defaultProps);

      branchesAndTagsListHelpers.openTab('tags', component);

      expect(branchesAndTagsListHelpers.getDisplayedItems(component)).toEqual(
        tags
      );
    });
  });

  it('should filter items by the fuzzy seach', () => {
    const tags = ['tag', 'tagba1', 'adfadf', 'tagba'];
    const component = makeComponent({
      ...defaultProps,
      tags,
    });

    branchesAndTagsListHelpers.openTab('tags', component);
    branchesAndTagsListHelpers.filterField.change('ba', component);

    expect(branchesAndTagsListHelpers.getDisplayedItems(component)).toEqual([
      tags[1],
      tags[3],
    ]);
  });

  describe('changing commit pointer', () => {
    it('should change commit pointer according to an active tab', () => {
      const onChangeCommitPointerSpy = jest.fn();
      let component = makeComponent({
        ...defaultProps,
        onChangeCommitPointer: onChangeCommitPointerSpy,
      });

      branchesAndTagsListHelpers.changeCommitPointer(branches[1], component);
      expect(onChangeCommitPointerSpy).toBeCalledWith(
        CommitPointerHelpers.makeFromBranch(branches[1])
      );

      onChangeCommitPointerSpy.mockReset();
      component = makeComponent({
        ...defaultProps,
        onChangeCommitPointer: onChangeCommitPointerSpy,
      });
      branchesAndTagsListHelpers.openTab('tags', component);
      branchesAndTagsListHelpers.changeCommitPointer(tags[1], component);
      expect(onChangeCommitPointerSpy).toBeCalledWith(
        CommitPointerHelpers.makeFromTag(tags[1])
      );
    });

    it('should close menu after changing commit pointer', () => {
      const component = makeComponent(defaultProps);

      branchesAndTagsListHelpers.openTab('branches', component);
      expect(branchesAndTagsListHelpers.isMenuOpen(component)).toBeTruthy();
      branchesAndTagsListHelpers.changeCommitPointer(branches[1], component);

      expect(branchesAndTagsListHelpers.isMenuOpen(component)).toBeFalsy();
    });
  });
});
