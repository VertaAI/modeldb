import React from 'react';

import {
  IRepository,
  IBranchesAndTags,
} from 'core/shared/models/Versioning/Repository';
import {
  CommitPointer,
  CommitPointerHelpers,
} from 'core/shared/models/Versioning/RepositoryData';
import withLoadingBranchesAndTags from '../repositoryData/view/RepositoryData/WithLoadingBranchesAndTags/WithLoadingBranchesAndTags';

interface IABCommitPointers {
  commitPointerA: CommitPointer;
  commitPointerB: CommitPointer;
}

interface IABCommitPointersValue {
  commitPointerAValue: CommitPointer['value'];
  commitPointerBValue: CommitPointer['value'];
}

interface IWrapperOwnProps {
  repository: IRepository;
}

export default function withABCommitPointers<
  Props extends IABCommitPointers & IBranchesAndTags & IWrapperOwnProps
>(WrappedComponent: React.ComponentType<Props>) {
  return ({
    commitPointerAValue,
    commitPointerBValue,
  }: IABCommitPointersValue): React.FC<
    Omit<Props, keyof IBranchesAndTags | keyof IABCommitPointers> &
      IWrapperOwnProps
  > => {
    const Wrapper: React.FC<Omit<Props, keyof IABCommitPointers>> = ({
      branches,
      tags,
      ...props
    }) => {
      const commitPointerA = CommitPointerHelpers.makeCommitPointerFromString(
        commitPointerAValue,
        { branches, tags }
      );
      const commitPointerB = CommitPointerHelpers.makeCommitPointerFromString(
        commitPointerBValue,
        { branches, tags }
      );
      return (
        <WrappedComponent
          {...props as any}
          branches={branches}
          tags={tags}
          commitPointerA={commitPointerA}
          commitPointerB={commitPointerB}
        />
      );
    };

    return withLoadingBranchesAndTags(Wrapper) as any;
  };
}
